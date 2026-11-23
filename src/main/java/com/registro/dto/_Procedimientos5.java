/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dto;

/**
 *
 * 
CREATE PROCEDURE [dbo].[sp_ListaFormasPagoActivo]    
AS    
BEGIN
    SET NOCOUNT ON;

    SELECT IDFormaPago, FormasPago, Estado 
    FROM FormasPago    
    WHERE [Estado] = 'Activo';
END  

GO
CREATE proc sp_Lista_TiposDoc  
as  
select [Documento_ID],[Descripcion],[Estado] from TIPO_DOCUMENTO where Estado='Activo'  
ORDER BY [Documento_ID]ASC  
  
GO
CREATE proc sp_ListaConfiguracionSistema    
as    
select [idConfiguracion],[FlagStockNegativo],[FlagAlertaS],[FlagAlertaCC],[FlagAlertaCP],[FlagAfectoIgv],
	   [FlagFormaPagoXComElectronico],[FlagActivaCodigoBarra][FlagEnvioDirecto] 
from dbo.ConfiguracionesSistema    
  
GO
  
      
CREATE  proc sp_ListaAperturaCierreCajaAbierto--'01','01'              
@CodEmp varchar(2)             
                
as                
select [idAperturaCierre],[FechaTurno],[FechaCaja],[HoraIni],[CodResp],[MontoInicial],[VentaDia],    
    [VentaContado],[VentaCredito],[VentaEfectivo],[VentaDeposito],[TotalPagos],[TotalGastos],    
    [TotalIngEfectivo],[HoraCierre],[EstadoCaja],[EfectivoActual],[Turno],[CodEmp]    
from dbo.AperturaCierreCaja where CodEmp=@CodEmp and EstadoCaja='A'          
      
    
GO
CREATE PROCEDURE [dbo].[sp_UltimaVentaTurno]--'01','2018-02-26','1'
    @CodEmp     VARCHAR(2),
    @FechaTurno DATETIME,
    @Turno      VARCHAR(2)
AS
BEGIN
    SET NOCOUNT ON;

    DECLARE @UltimaVenta DATETIME;
    DECLARE @Horas DECIMAL(9,2);

    -- 1. Obtener la última venta de manera óptima (Sargable para usar índices)
    SELECT @UltimaVenta = MAX(FechaHora)
    FROM dbo.Cabecera_Ventas
    WHERE CodEmp = @CodEmp
      AND FechaTurno >= CAST(@FechaTurno AS DATE)
      AND FechaTurno <  DATEADD(DAY, 1, CAST(@FechaTurno AS DATE))
      AND LTRIM(RTRIM(Turno)) = LTRIM(RTRIM(@Turno))
      -- Una alternativa más rápida a convertir a TIME si FechaHora está indexado:
      AND FechaHora <> CAST(CAST(FechaHora AS DATE) AS DATETIME); 

    -- 2. Obtener la hora de bloqueo de forma directa (Sin SQL dinámico)
    -- Si las columnas varían entre ambientes, es mejor unificar la tabla, pero si no se puede,
    -- un bloque estático COALESCE maneja los diferentes nombres de columna limpiamente:
    SET @Horas=(SELECT HoraBloqueoTurno FROM dbo.ParametrosEmpresa
    WHERE (LTRIM(RTRIM(CodEmpresa)) = @CodEmp))
        -- Si la tabla no tiene ninguna de las dos columnas, remueve el WHERE.

    -- 3. Retornar el resultado
    SELECT 
        UltimaVenta  = @UltimaVenta, 
        HorasBloqueo = ISNULL(@Horas, 0.00);
END

GO
-- sp_InsertaNumeracion  
CREATE   PROCEDURE sp_InsertaNumeracion --2    
  
as    
 DECLARE @Pedido_ID VARCHAR(10)     
 DECLARE @max int    
    
 IF(SELECT MAX(Pedido_ID)from NumeracionOP)<>0    
    
    BEGIN    
      
  select @Pedido_ID=max(Pedido_ID) from dbo.NumeracionOP    
    
  END    
 ELSE    
    BEGIN    
  select @Pedido_ID=max(Pedido_ID) from dbo.Cabecera_Ventas    
      
  END    
     
  set @Pedido_ID=isnull(@Pedido_ID,'0000000000')    
  set @max=convert(integer,right(@Pedido_ID,10))+1    
  set @Pedido_ID='0'+right('000000000'+convert(Varchar(10),@max),9)    
    
  
BEGIN TRANSACTION    
    
 IF (SELECT COUNT(*)    
    FROM NumeracionOP    
    WHERE Pedido_ID=@Pedido_ID)=0    
    BEGIN    
  insert into NumeracionOP     
   values(@Pedido_ID)    
      
 select max(Pedido_ID) from dbo.NumeracionOP    
  END    
  IF @@ERROR<>0    
     BEGIN    
   ROLLBACK TRANSACTION    
   RETURN -1    
   END    
    ELSE    
    BEGIN     
   COMMIT TRANSACTION    
   RETURN 0    
   END  
GO
create proc sp_autogenera_cod_BoletaVenta  
as  
select '0'+ replicate('0',8-(len(isnull(cast(right(max(Boleta_id),7)as int),0)+1)+1))  
+ cast(isnull(cast(right(max(Boleta_id),7)as int),0)+1 as char(8))from Boleta  
  
GO
CREATE proc sp_numero_SerieBoleta    
as    
select ISNULL(max(NroSerie),'001')from dbo.Boleta     
    
GO

CREATE   PROCEDURE [dbo].[sp_ConfiguracionSeries_Siguiente]--'01','02'
    @CodEmp       varchar(2),
    @Documento_ID varchar(2)
AS
BEGIN
    SET NOCOUNT ON;
    SELECT TOP 1
           NroSerie     = RTRIM(cs.NroSerie),
           NroDocumento = RIGHT('0000000' + CAST(ISNULL(cs.Correlativo, 0) + 1 AS varchar(7)), 7)
    FROM dbo.ConfiguracionSeries cs
    WHERE cs.CodEmp = @CodEmp
      AND cs.Documento_ID = @Documento_ID
      AND ISNULL(cs.Estado, 'A') = 'A';
END

GO
CREATE  proc sp_lista_ClienteXcod --'000001'      
@cliente_id char(8)      
as      
select Cliente_ID,RazonSocial,Direccion,Telefono,Estado,Telefono2,Correo,Sexo,RucDNI,TipoId,    
       Foto,Contacto,TelefonoCont, HuellaDactilar    
from cliente   
where cliente_id=@cliente_id      
      
  
GO
CREATE PROC [dbo].[sp_Lista_ProductosGral_ActivosXNom]
    @CodEmp VARCHAR(2),  
    @NombreProducto varchar(50)  
AS  
BEGIN
    SET NOCOUNT ON; -- Reduce el tráfico de red y overhead en el servidor

    SELECT 
        p.Producto_ID,
        p.Descripcion,
        p.UnidadMedida,
        p.StockFinal,
        p.PrecioCosto,
        p.PrecioVenta,
        p.PrecioLimite,  
        p.NombreProducto,
        Categoria = c.nombre,
        Marca = m.nombre,  
        p.MargenUtil,
        Peso = ISNULL(p.Peso, 0),
        p.Imagen,
        p.CodigoBarra    
    FROM dbo.productos AS p WITH (NOLOCK) -- Evita bloqueos concurrentes mientras los cajeros buscan productos
    INNER JOIN dbo.marcaproducto AS m ON m.MarcaProducto_ID = p.MarcaProducto_ID  
    INNER JOIN dbo.categoria AS c ON c.Categoria_ID = p.Categoria_ID  
    WHERE p.EstadoProducto = 'Activo'   
      AND p.CodEmp = @CodEmp   
      AND p.Descripcion LIKE '%'+@NombreProducto + '%' 
    ORDER BY p.Descripcion ASC, p.NombreProducto ASC;
END;

GO
  
CREATE  proc sp_Lista_ProductosGral_ActivosXCodBarra--'s'    
@CodEmp VARCHAR(2),    
@CodigoBarra varchar(50)    
as    
select distinct Producto_ID,p.Descripcion,UnidadMedida,StockFinal,PrecioCosto,PrecioVenta,PrecioLimite,    
                NombreBusqueda=(NombreProducto),Categoria=(c.nombre),Marca=(m.nombre),    
                MargenUtil,Peso=isnull(Peso,0),Imagen,CodigoBarra      
from productos as p    
INNER JOIN marcaproducto as m ON m.MarcaProducto_ID=p.MarcaProducto_ID    
INNER JOIN categoria as c ON c.Categoria_ID=p.Categoria_ID    
where EstadoProducto='Activo'     
  and CodEmp=@CodEmp     
  and CodigoBarra=@CodigoBarra    
  
GO
  
CREATE proc sp_listaTipoProductoXCodigo  
@CodEmp VARCHAR(2),  
@Producto_ID char(8)  
as  
  
select TipoProd from dbo.Productos where CodEmp=@CodEmp and Producto_ID=@Producto_ID  
  
GO
  
CREATE  proc sp_InsertaNumeracionComp --1    
@TipDoc varchar(2)    
as    
    
 DECLARE @Venta_id CHAR(8)    
 DECLARE @max int    
    
BEGIN TRANSACTION    
    
 IF @TipDoc='01'  
    BEGIN    
    IF(SELECT MAX(Venta_ID)from dbo.NumeracionComp1)<>0    
       BEGIN    
    
  select @Venta_id=max(Venta_ID) from dbo.NumeracionComp1    
      
  END    
 ELSE    
    BEGIN    
  select @Venta_id=max(Nota_Venta_ID) from nota_venta    
  END    
    
  set @Venta_id=isnull(@Venta_id,'00000000')    
  set @max=convert(integer,right(@Venta_id,7))+1    
  set @Venta_id='0'+right('0000000'+convert(VARchar(8),@max),7)    
    
 BEGIN    
  IF (SELECT COUNT(*)    
     FROM NumeracionComp1    
     WHERE Venta_ID=@Venta_id)=0    
  BEGIN    
  insert into NumeracionComp1     
   values(@Venta_id)    
      END    
    
  select max(Venta_ID) from NumeracionComp1    
    END    
    
    END    
     
 ELSE    
 IF @TipDoc='02'  
    BEGIN    
    IF(SELECT MAX(Venta_ID)from dbo.NumeracionComp2)<>0    
  BEGIN    
    
  select @Venta_id=max(Venta_ID) from dbo.NumeracionComp2    
    
  END    
 ELSE    
    BEGIN    
  select @Venta_id=max(Boleta_ID) from dbo.Boleta    
  END    
    
  set @Venta_id=isnull(@Venta_id,'00000000')    
  set @max=convert(integer,right(@Venta_id,7))+1    
  set @Venta_id='0'+right('0000000'+convert(VARchar(8),@max),7)    
    
    BEGIN    
  IF (SELECT COUNT(*)    
     FROM NumeracionComp2    
     WHERE Venta_ID=@Venta_id)=0    
  BEGIN    
  insert into NumeracionComp2     
   values(@Venta_id)    
   END    
  select max(Venta_ID) from NumeracionComp2    
     END    
    END    
 ELSE    
 IF @TipDoc='03'   
     BEGIN    
    IF(SELECT MAX(Venta_ID)from dbo.NumeracionComp3)<>0    
  BEGIN    
      
  select @Venta_id=max(Venta_ID) from NumeracionComp3    
    
  END    
 ELSE    
    BEGIN    
     
  select @Venta_id=max(Factura_ID) from dbo.Factura    
    END    
    
  set @Venta_id=isnull(@Venta_id,'00000000')    
  set @max=convert(integer,right(@Venta_id,7))+1    
  set @Venta_id='0'+right('0000000'+convert(VARchar(8),@max),7)    
    
  BEGIN    
  IF (SELECT COUNT(*)    
     FROM NumeracionComp3    
     WHERE Venta_ID=@Venta_id)=0    
  BEGIN    
  insert into NumeracionComp3     
   values(@Venta_id)    
   END    
  select max(Venta_ID) from NumeracionComp3    
     END     
 END     
      
    
  IF @@ERROR<>0    
    
     BEGIN    
     ROLLBACK TRANSACTION    
     RETURN -1    
     END    
    ELSE    
 BEGIN     
    COMMIT TRANSACTION    
    RETURN 0    
    END    
    
GO
CREATE PROCEDURE [dbo].[sp_Insert_CabeceraVentas]      
 @Pedido_ID char(10),        
 @Tipo_documento varchar(30),        
 @Venta_id char(8),        
 @NroSerie char(5),        
 @Nro_Doc char(8),        
 @Cliente_ID char(8),        
 @RazonSocial varchar(100),        
 @Fecha_Pedido datetime,        
 @CondPago varchar(30),        
 @FormaPago varchar(30),        
 @SubTotal money,        
 @IGV money,        
 @Total money,        
 @Estado char(10),        
 @CodEmp varchar(2),        
 @MontoLetras VARCHAR(100),        
 --@NroGuia varchar(15), -- Nota: Este parámetro sigue en la cabecera pero ya no se inserta en Cabecera_Ventas según tu versión.     
 @CodPer int,    
 @Dia varchar(2),    
 @FechaTurno datetime,    
 @FechaHora datetime,    
 @Turno varchar(2),  
 @TipoDoc varchar(2),
 @IDFormaPago varchar(2),  
 @CodUser int  
  
AS        
BEGIN
    -- Configuraciones para mejorar rendimiento y control estricto de transacciones
    SET NOCOUNT ON;
    SET XACT_ABORT ON; 

    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE @TipoDetraccion INT = 0;      
        DECLARE @detraccion_total decimal(20,2) = 0;  
        DECLARE @detraccion_porcentaje decimal(20,2) = 0;   
        
        -- 1. Registro en Auditoría del Sistema
        INSERT INTO [dbo].[Auditoria_Sistema]([Id_UserSist],[FechaRegistro],[Cadena],[accion],[registro_id])    
        VALUES (@CodUser, GETDATE(), 'Registrar Venta del dia afectando a las tablas: Orden_Pedido, Tipos de Comprobantes, VentaCredito, DetalleFormasPago, Productos y Almacen', 'Graba la Venta', @Pedido_ID);
  
        ---- Estandarizamos el tipo de documento a mayúsculas para evitar fallos de case-sensitivity
        --SET @Tipo_documento = UPPER(TRIM(@Tipo_documento));

        -- 2. Inserciones específicas según el Tipo de Documento
        IF @Tipo_documento = 'NOTA DE VENTA'        
        BEGIN        
            SELECT @Nro_Doc = @Pedido_ID;    
            
            IF NOT EXISTS (SELECT 1 FROM Nota_Venta WHERE CodEmp = @CodEmp AND Nota_Venta_ID = @Venta_id AND Pedido_ID = @Pedido_ID)        
            BEGIN        
                INSERT INTO Nota_Venta (Nota_Venta_ID, Pedido_ID, Cliente_ID, Fecha, CondPago, FormaPago, Total, Estado, CodEmp, CodPer)        
                VALUES (@Venta_id, @Pedido_ID, @Cliente_ID, @Fecha_Pedido, @CondPago, @FormaPago, @Total, @Estado, @CodEmp, @CodPer);        
            END        
        END        
        ELSE IF @Tipo_documento = 'BOLETA DE VENTA'        
        BEGIN        
            IF NOT EXISTS (SELECT 1 FROM Boleta WHERE CodEmp = @CodEmp AND Boleta_ID = @Venta_id AND Pedido_ID = @Pedido_ID)        
            BEGIN        
                INSERT INTO dbo.Boleta (Boleta_ID, Pedido_ID, NroSerie, NroDocB, Cliente_ID, FechaEmi, FechaVen, ConPago, FormaPago, SubTotal, Total, MontoLetras, Estado, CodEmp, CodPer)        
                VALUES (@Venta_id, @Pedido_ID, @NroSerie, @Nro_Doc, @Cliente_ID, @Fecha_Pedido, @Fecha_Pedido, @CondPago, @FormaPago, @SubTotal, @Total, @MontoLetras, @Estado, @CodEmp, @CodPer);        
            END        
        END        
        ELSE IF @Tipo_documento = 'FACTURA'        
        BEGIN        
            IF NOT EXISTS (SELECT 1 FROM factura WHERE CodEmp = @CodEmp AND Factura_ID = @Venta_id AND Pedido_ID = @Pedido_ID)        
            BEGIN        
                INSERT INTO factura (Factura_ID, Pedido_ID, NroSerie, NroDocF, Cliente_ID, FechaEmi, FechaVen, ConPago, FormaPago, SubTotal, IGV, Total, MontoLetras, Estado, CodEmp, CodPer)        
                VALUES (@Venta_id, @Pedido_ID, @NroSerie, @Nro_Doc, @Cliente_ID, @Fecha_Pedido, @Fecha_Pedido, @CondPago, @FormaPago, @SubTotal, @IGV, @Total, @MontoLetras, @Estado, @CodEmp, @CodPer);        
            END        
        END        
          
        -- 3. Inserción en Cabecera_Ventas (Se incluye @TipoDoc al final como agregaste en tu código)
        IF NOT EXISTS (SELECT 1 FROM Cabecera_Ventas WHERE CodEmp = @CodEmp AND Pedido_ID = @Pedido_ID AND Venta_id = @Venta_id)        
        BEGIN        
            -- NOTA: Se recomienda mapear explícitamente las columnas de Cabecera_Ventas en producción
            INSERT INTO Cabecera_Ventas         
            VALUES (@Pedido_ID, @Tipo_documento, @Venta_id, @NroSerie, @Nro_Doc, @Cliente_ID, @RazonSocial, @Fecha_Pedido, @CondPago, @FormaPago, @SubTotal, @IGV, @Total, @Estado, @CodEmp, 'NO', @MontoLetras, @CodPer, @Dia, @FechaTurno, @FechaHora, @Turno, @TipoDoc);        
        END        
    
        ---- 4. Procesos Contables Externos (Excluyendo Notas de Venta)
        --IF @Tipo_documento <> 'NOTA DE VENTA'                                 
        --BEGIN                                
        --    EXEC sp_InsertCont_CabCompVentas @CodEmp, @Pedido_ID, @Fecha_Pedido, @Cliente_ID, @TipoDetraccion, @detraccion_total, @detraccion_porcentaje;                  
        --    EXEC sp_InsertCont_DetCompVentas @CodEmp, @Pedido_ID;                                 
        --END   
  
        -- 5. Gestión del Detalle de Formas de Pago
        IF EXISTS (SELECT 1 FROM [TempDetalleFormasPago] WHERE Pedido_ID = @Pedido_ID AND CodEmp = @CodEmp)            
        BEGIN            
            IF NOT EXISTS (SELECT 1 FROM [DetalleFormasPago] WHERE Pedido_ID = @Pedido_ID AND CodEmp = @CodEmp)            
            BEGIN            
                INSERT INTO [DetalleFormasPago] ([IDFormaPago], [Pedido_ID], [FormaPago], [MontoParcial], [PorcInc], [MontoInc], [MontoReal], [IDBanco], [NroOperacion], [CodEmp])                
                SELECT [IDFormaPago], [Pedido_ID], [FormaPago], [MontoParcial], [PorcInc], [MontoInc], [MontoReal], [IDBanco], [NroOperacion], [CodEmp]                
                FROM [dbo].[TempDetalleFormasPago]                
                WHERE Pedido_ID = @Pedido_ID AND [CodEmp] = @CodEmp;      
    
                DELETE FROM [TempDetalleFormasPago] WHERE Pedido_ID = @Pedido_ID AND [CodEmp] = @CodEmp;     
            END            
        END            
        ELSE            
        BEGIN            
            IF NOT EXISTS (SELECT 1 FROM [DetalleFormasPago] WHERE Pedido_ID = @Pedido_ID AND CodEmp = @CodEmp)            
            BEGIN            
                -- NOTA: Se recomienda mapear explícitamente las columnas de DetalleFormasPago aquí
                INSERT INTO [DetalleFormasPago]                
                VALUES (@IDFormaPago, @Pedido_ID, 'Efectivo', @Total, 0, @Total, @Total, '01', '', @CodEmp);               
            END            
        END               
  
        -- Si todas las instrucciones se completaron con éxito, confirmamos los datos
        COMMIT TRANSACTION;        
        RETURN 0;        

    END TRY
    BEGIN CATCH
        -- Si ocurre cualquier error dentro del bloque, se deshacen todos los cambios de inmediato
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;        
        
        -- Lanzamos el error original para facilitar el debug desde la aplicación cliente
        DECLARE @ErrorMessage NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
        DECLARE @ErrorState INT = ERROR_STATE();
        RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);

        RETURN -1;        
    END CATCH
END


GO
CREATE PROCEDURE [dbo].[sp_ConfiguracionSeries_Actualiza]
    @CodEmp       varchar(2),
    @Documento_ID varchar(2),
    @NroSerie     varchar(4),
    @NroDocumento varchar(8)
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE dbo.ConfiguracionSeries
       SET Correlativo = CAST(@NroDocumento AS int)
     WHERE CodEmp = @CodEmp
       AND Documento_ID = @Documento_ID
       AND RTRIM(@NroSerie) = RTRIM(@NroSerie)
       AND ISNULL(Correlativo, 0) < CAST(@NroDocumento AS int);
END

GO
CREATE PROCEDURE [dbo].[sp_Insert_Detalle_Ventas]      
 @NroPedido char(10),      
 @Venta_ID char(8),      
 @Producto_ID char(6),      
 @Descripcion varchar(100),      
 @PrecioCosto money,     
 @PrecioUnitario money,      
 @Cantidad real,      
 @Importe money,      
 @TipoDocumento varchar(30),  
 @CodEmp varchar(2),  
 @FechaTurno datetime,  
 @Turno varchar(2)  
AS      
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON; -- Cancela automáticamente todo si hay un error de llaves o tipos de datos

    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE @UDECIMAL REAL;      
        DECLARE @PVentaSalida money;      
        DECLARE @Saldo real;      
        DECLARE @SaldoAlmacen real;      
        DECLARE @SaldoTienda real;      
        DECLARE @AfectoIGV varchar(2);      
        DECLARE @ValIgv decimal(10,2);   

        -- Obtención de configuraciones del sistema
        SELECT @AfectoIGV = [FlagAfectoIgv] FROM [dbo].[ConfiguracionesSistema];
        SELECT @ValIgv = ValorIGV FROM [ParametrosEmpresa];

        -- Validamos que la cabecera exista de verdad
        IF EXISTS (SELECT 1 FROM dbo.Cabecera_Ventas WHERE Pedido_ID = @NroPedido)      
        BEGIN      
      
            -- 1. Gestión en Detalle_Ventas (General)
            IF NOT EXISTS (SELECT 1 FROM Detalle_Ventas WHERE Producto_ID = @Producto_ID AND Pedido_ID = @NroPedido)      
            BEGIN      
                INSERT INTO Detalle_Ventas([Pedido_ID], [Producto_ID], [Descripcion], [PrecioCosto], [PrecioUnitario], [Cantidad], [Importe], [SubTotalD], [IgvD])       
                VALUES (
                    @NroPedido, 
                    @Producto_ID, 
                    @Descripcion, 
                    @PrecioCosto, 
                    @PrecioUnitario, 
                    @Cantidad, 
                    @Importe,
                    (CASE @AfectoIGV WHEN 'S' THEN (@Importe / ((@ValIgv / 100) + 1)) ELSE @Importe END),
                    (CASE @AfectoIGV WHEN 'S' THEN (@Importe - (@Importe / ((@ValIgv / 100) + 1))) ELSE 0 END)
                );
            END        
            ELSE      
            BEGIN      
                UPDATE Detalle_Ventas       
                SET Descripcion = @Descripcion,
                    PrecioCosto = @PrecioCosto,
                    PrecioUnitario = @PrecioUnitario,    
                    Cantidad = @Cantidad,
                    Importe = @Importe      
                WHERE Pedido_ID = @NroPedido AND Producto_ID = @Producto_ID;      
            END      

            ---- Estandarizamos texto para evitar conflictos de intercalación (Case Sensitivity)
            --SET @TipoDocumento = UPPER(TRIM(@TipoDocumento));

            -- 2. Gestión por Tipos de Comprobante específicos
            IF @TipoDocumento = 'NOTA DE VENTA'      
            BEGIN      
                IF NOT EXISTS (SELECT 1 FROM Detalle_Nota_Venta WHERE Producto_ID = @Producto_ID AND Nota_Venta_ID = @Venta_ID)      
                BEGIN      
                    -- Se recomienda listar columnas en producción: INSERT INTO Detalle_Nota_Venta(Venta_ID, ...)
                    INSERT INTO dbo.Detalle_Nota_Venta VALUES (@Venta_ID, @Producto_ID, @Descripcion, @PrecioUnitario, @Cantidad, @Importe);      
                END      
                ELSE      
                BEGIN      
                    UPDATE Detalle_Nota_Venta 
                    SET Descripcion = @Descripcion,
                        PrecioUnitario = @PrecioUnitario,      
                        Cantidad = @Cantidad,
                        Importe = @Importe      
                    WHERE Producto_ID = @Producto_ID AND Nota_Venta_ID = @Venta_ID;      
                END      
            END      
            ELSE IF @TipoDocumento = 'BOLETA DE VENTA'      
            BEGIN      
                IF NOT EXISTS (SELECT 1 FROM Detalle_Boleta WHERE Producto_ID = @Producto_ID AND Boleta_ID = @Venta_ID)      
                BEGIN      
                    INSERT INTO dbo.Detalle_Boleta VALUES (@Venta_ID, @Producto_ID, @Descripcion, @PrecioUnitario, @Cantidad, @Importe);      
                END      
                ELSE      
                BEGIN      
                    UPDATE Detalle_Boleta 
                    SET Descripcion = @Descripcion,
                        PrecioUnitario = @PrecioUnitario,      
                        Cantidad = @Cantidad,
                        Importe = @Importe      
                    WHERE Producto_ID = @Producto_ID AND Boleta_ID = @Venta_ID;      
                END      
            END      
            ELSE IF @TipoDocumento = 'FACTURA'      
            BEGIN      
                IF NOT EXISTS (SELECT 1 FROM Detalle_Factura WHERE Producto_ID = @Producto_ID AND Factura_ID = @Venta_ID)      
                BEGIN      
                    INSERT INTO dbo.Detalle_Factura VALUES (@Venta_ID, @Producto_ID, @Descripcion, @PrecioUnitario, @Cantidad, @Importe);      
                END      
                ELSE      
                BEGIN      
                    UPDATE Detalle_Factura 
                    SET Descripcion = @Descripcion,
                        PrecioUnitario = @PrecioUnitario,      
                        Cantidad = @Cantidad,
                        Importe = @Importe      
                    WHERE Producto_ID = @Producto_ID AND Factura_ID = @Venta_ID;      
                END      
            END      
         
            -- 3. Actualización de Stock Maestro
            UPDATE dbo.Productos 
            SET StockFinal = (StockFinal - @Cantidad)    
            WHERE Producto_ID = @Producto_ID;      
    
            ---- 4. Actualización del Almacén Temporal por Turno (Validación exacta por Producto)
            --IF EXISTS (
            --    SELECT 1 
            --    FROM dbo.TempMovimientoDia_Almacen  
            --    WHERE CodEmp = @CodEmp AND FechaMovimiento = @FechaTurno AND Turno = @Turno AND Producto_ID = @Producto_ID
            --)  
            --BEGIN  
            --    UPDATE dbo.TempMovimientoDia_Almacen   
            --    SET CantSalida = (CantSalida + @Cantidad),
            --        SaldoFinal = (SaldoFinal - @Cantidad)  
            --    WHERE CodEmp = @CodEmp  
            --      AND FechaMovimiento = @FechaTurno  
            --      AND Turno = @Turno  
            --      AND Producto_ID = @Producto_ID;  
            --END  
      
        END -- Fin del bloque IF Cabecera

        COMMIT TRANSACTION;        
        RETURN 0;

    END TRY
    BEGIN CATCH
        -- Si cualquier query falla, la transacción se revierte al 100% protegiendo el stock
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;      
        
        -- Propagamos el error para auditoría de desarrollo
        DECLARE @ErrorMessage NVARCHAR(4000) = ERROR_MESSAGE();
        DECLARE @ErrorSeverity INT = ERROR_SEVERITY();
        DECLARE @ErrorState INT = ERROR_STATE();
        RAISERROR(@ErrorMessage, @ErrorSeverity, @ErrorState);

        RETURN -1;
    END CATCH
END


GO
CREATE proc  [dbo].[sp_InsertCont_CabCompVentas]-- '01','00005081','2026-03-06 00:00:00.000','70371456' ,0,0,0       
@CodEmp varchar(2),        
@Pedido_ID varchar(10),        
@FechaVen DATETIME,        
@RucDniCliente VARCHAR(12)--,      
--@TipoDetraccion INT,      
--@detraccion_total decimal(20,2),      
--@detraccion_porcentaje decimal(20,2)      
      
as        
        
DECLARE @IDDOCUMENTO VARCHAR(10)        
DECLARE @RUCEMISOR VARCHAR(11)        
DECLARE @TipoDocCte VARCHAR(10)        
DECLARE @TIPODOCIDENTIDADCLIENTE VARCHAR(10)        
             
SELECT @IDDOCUMENTO=(SELECT RIGHT('0000000000' + CAST(ISNULL(MAX(CAST(RIGHT(IDDOCUMENTO, 10) AS INT)), 0) + 1 AS VARCHAR(10)), 10)      
FROM Cont_CabCompVentas WHERE CodEmp=@CodEmp)      
      
SELECT @RUCEMISOR=(SELECT Ruc FROM dbo.Empresa WITH(NOLOCK)WHERE Codigo_Emp=@CodEmp)      
      
SET @TipoDocCte=(SELECT TOP 1 TipoId FROM dbo.Cliente WITH(NOLOCK)WHERE Cliente_ID=@RucDniCliente)      
      
SELECT @TIPODOCIDENTIDADCLIENTE=(CASE @TipoDocCte WHEN 'RUC'THEN '06'      
                                                  WHEN 'DNI'THEN '01'ELSE'00'END)      
        
INSERT INTO Cont_CabCompVentas        
    (Pedido_ID,IDDOCUMENTO,TIPODOCUMENTO,TIPODOCINCLUIDO,RUCEMISOR,RUCCLIENTE,FECHAEMISION,FECHAVENCIMIENTO,        
  TIPOMONEDA,SUBTOTAL,TOTALDESCUENTOS,TOTALBASE,TOTALISC,TOTALIGV,TOTALOTROSCARGOS,TOTALOTROSTRIBUTOS,        
  IMPORTETOTAL,TIENEDOCREF,TIPODOCREF,NRODOCREF,NROSERIE,NRODOCUMENTO,USUARIOID,NREFERENCIAINT,        
  TIPODOCIDENTIDADCLIENTE,TIPODEFACTURA,MENSAJEDETRACCION,CENTRODECOSTO,NUMERODENEGOCIO,TRANSGRATUITA,        
  DESCGLOBAL,ANTICIPO,NÚMERODEANTICIPO,MONTOANTICIPO,PERCEPCION,REGIMENDEPERCEPCION,TASADEPERCEPCION,        
  MONTODEPERCEPCION,TOTALACOBRAR,CodEmp,EstadoEnvio,motivoRechazo,TOTALINAFECTA,TOTALEXONERADA,            
  TOTALGRATUITA,ICBPER,detraccion_total,detraccion_porcentaje,TipoCambio,IGVPor)        
           
SELECT p.Pedido_ID,@IDDOCUMENTO,(CASE TipoDoc WHEN '02'THEN 'BO'ELSE 'FA'END),TipoDoc,@RUCEMISOR,@RucDniCliente,      
       Fecha_Pedido,@FechaVen,        
       'PEN',SubTotal,0,SubTotal,TOTALISC=0,IGV,TOTALOTROSCARGOS=0,TOTALOTROSTRIBUTOS=0,Total,'0','NO','',        
        NroSerie,Nro_Documento,CodPer,Venta_ID,@TIPODOCIDENTIDADCLIENTE,'01',      
       0,'001','100','00','00','00','',0,'0','01',        
        0,0,Total,p.CodEmp,'','',0,0.00,0,0,0,0,1,18       
        --'C',RUC=(cli.Cliente_ID),cli.RazonSocial,Direccion,Telefono1,Correo,(CASE Pais_ID WHEN 1 THEN 'PE'END),Region=Provincia,Distrito,        
        --CONTACTOFACTURACION=(Referencia),NOMBRECOMERCIAL=Nombres,@TIPODOCIDENTIDADCLIENTE        
--FROM dbo.Orden_Pedido op,dbo.Cliente cli        
--WHERE op.Cliente_ID=cli.Cliente_ID        
--  AND @CodEmp=@CodEmp        
--  AND Pedido_ID=@Pedido_ID        
        
FROM dbo.Cabecera_Ventas p WITH(NOLOCK)            
WHERE p.CodEmp=@CodEmp        
  AND p.Pedido_ID=@Pedido_ID    
  --AND p.tiporeserva='AGRUPADA'        
      
UPDATE Cont_CabCompVentas SET TOTALEXONERADA=0.00  where Pedido_ID=@Pedido_ID      

GO

CREATE proc sp_InsertCtasXCobrar  
 @Cliente_ID varchar(20),  
 @NroRegistro char(10),  
 @FechaEmi datetime,  
 @TipoDocumento varchar(20),  
 @NroDocumento varchar(20),  
 @Monto money,  
 @NroDias int,  
 @Interes money,  
 @Personal_ID int ,  
 @SaldoCredito money ,  
 @CondicionCredito varchar(30),  
 @FechaCanc datetime,  
 @CodEmp varchar(2),  
 @UsuarioMod int,  
 @FechaVenc datetime,  
 @CondCli varchar(20)  
  
as  
  
BEGIN TRANSACTION  
  
DECLARE @Cobro_ID varchar(8)    
  
IF @CondCli='CREDITO'    
 BEGIN    
    
 SELECT @Cobro_ID=(select '0'+replicate('0',8-(len(isnull(cast(right(max(Cobro_ID),8 )as int),0)+1)+1))    
 + cast(isnull(cast(right(max(Cobro_ID), 8)as int),0)+1 as varchar(8))from CtasXCobrar    
 where CodEmp=@CodEmp)    
    
 IF(SELECT COUNT(*)    
    FROM CtasXCobrar    
    WHERE NroRegistro=@NroRegistro    
      and CodEmp=@CodEmp)=0    
    
    BEGIN    
  insert into CtasXCobrar     
  values(@Cobro_ID,@Cliente_ID,@NroRegistro,@FechaEmi,@TipoDocumento,@NroDocumento,@Monto,@NroDias,  
         @Interes,@Personal_ID,@SaldoCredito,@CondicionCredito,@FechaCanc,@CodEmp,@UsuarioMod,@FechaVenc,0)    
    
  END    
     ELSE    
    
    BEGIN    
 SELECT @Cobro_ID=(select Cobro_ID from dbo.CtasXCobrar where NroRegistro=@NroRegistro and CodEmp=@CodEmp)    
    
  update CtasXCobrar set Cliente_ID=@Cliente_ID,NroRegistro=@NroRegistro,FechaEmi=@FechaEmi,  
                         TipoDocumento=@TipoDocumento,NroDocumento=@NroDocumento,Monto=@Monto,  
                         NroDias=@NroDias,Interes=@Interes,Personal_ID=@Personal_ID,  
                         CondicionCredito=@CondicionCredito, SaldoCredito=@SaldoCredito,    
                         FechaCanc=@FechaCanc,FechaVenc=@FechaVenc    
  where Cobro_ID=@Cobro_ID     
    AND CodEmp=@CodEmp    
    
    
  END    
     END    
    ELSE    
 BEGIN    
   
 delete from dbo.CtasXCobrar     
  where Cobro_ID=@Cobro_ID     
    AND CodEmp=@CodEmp     
    
  delete from dbo.DetalleCtasXCobrar  
  where @Cobro_ID=@Cobro_ID    
    
    END    
 IF @@ERROR <> 0    
  BEGIN    
  ROLLBACK TRANSACTION    
  RETURN -1    
  END    
    ELSE    
  BEGIN    
  COMMIT TRANSACTION    
  RETURN 0    
  END    

GO

CREATE proc sp_ListaIDDOCUMENTOXNroPedido      
@CodEmp varchar(2),          
@Pedido_ID varchar(10),      
@TIPODOCUMENTO varchar(2)      
      
as      
      
select IDDOCUMENTO from dbo.Cont_CabCompVentas       
where CodEmp=@CodEmp       
  and Pedido_ID=@Pedido_ID      
  and TIPODOCUMENTO=@TIPODOCUMENTO      
      
    
  
GO

CREATE   PROCEDURE [dbo].[sp_ListaOrdenPedidoImpresoXPed]--'00000002'
@Pedido_ID char(10)
as
select * from dbo.Cabecera_Ventas where Pedido_ID=@Pedido_ID
AND Impreso='SI'AND Tipo_documento<>'NOTA DE VENTA'
GO


-- sp_EliminaVenta  
CREATE   PROCEDURE sp_EliminaVenta    
@Pedido_ID char(10),    
@Venta_ID char(10),    
@Tipo_doc varchar(2),  
@CodEmp varchar(2),  
@FechaTurno datetime,  
@Turno varchar(2)    
    
AS    
    
BEGIN TRANSACTION    
    
  UPDATE dbo.Productos SET StockFinal=(StockFinal+Cantidad)  
  FROM Productos P,dbo.Detalle_Ventas DOP  
  WHERE P.Producto_ID=DOP.Producto_ID  
    and DOP.Pedido_ID=@Pedido_ID  
    
  IF(SELECT COUNT(*)  
      FROM dbo.TempMovimientoDia_Almacen  
      WHERE CodEmp=@CodEmp  
        and FechaMovimiento=@FechaTurno  
        and Turno=@Turno)>0  
          
   BEGIN  
     
   UPDATE dbo.TempMovimientoDia_Almacen   
   SET CantSalida=(CantSalida-Cantidad),SaldoFinal=(SaldoFinal+Cantidad)  
   FROM TempMovimientoDia_Almacen tma,dbo.Detalle_Ventas DOP  
   WHERE tma.Producto_ID=DOP.Producto_ID  
     and DOP.Pedido_ID=@Pedido_ID  
     and CodEmp=@CodEmp  
     and FechaMovimiento=@FechaTurno  
     and Turno=@Turno  
          
    END  
      
  DELETE FROM  dbo.Cabecera_Ventas    
   WHERE Pedido_ID=@Pedido_ID    
  DELETE FROM dbo.Detalle_Ventas    
   WHERE Pedido_ID=@Pedido_ID    
    
  IF @Tipo_doc='01'    
   BEGIN    
   DELETE FROM Boleta    
   WHERE Boleta_ID=@Venta_ID    
    
   DELETE FROM dbo.Detalle_Boleta    
   WHERE Boleta_ID=@Venta_ID    
  END    
   ELSE    
  IF @Tipo_doc='02'    
   BEGIN    
   DELETE FROM Factura    
   WHERE Factura_ID=@Venta_ID    
       
   DELETE FROM dbo.Detalle_Factura    
   WHERE Factura_ID=@Venta_ID    
  END    
   ELSE    
    
  IF @Tipo_doc='03'    
   BEGIN    
   DELETE FROM Nota_Venta     
   WHERE Nota_Venta_ID=@Venta_ID    
    
   DELETE FROM dbo.Detalle_Nota_Venta    
   WHERE Nota_Venta_ID=@Venta_ID    
  END    
       
    
 IF @@ERROR <> 0    
   BEGIN    
   ROLLBACK TRANSACTION    
   RETURN -1    
   END    
  ELSE    
   BEGIN    
   COMMIT TRANSACTION    
   RETURN 0    
   END  
GO
  
CREATE   PROCEDURE sp_Update_CabeceraVentas-- 'NOTA DE VENTA','0000 0852','000004','40424328','1','26/03/2009','C','','1',8.5,0,0,0,0,8.5,'Efectiva',18  
 @Pedido_ID char(10),  
 @Tipo_documento varchar(30),  
 @Venta_id char(8),  
 @NroSerie char(3),  
 @Nro_Doc char(8),  
 @Cliente_ID varchar(20),  
 @RazonSocial varchar(100),  
 @Fecha_Pedido datetime,  
 @CondPago varchar(20),  
 @FormaPago varchar(20),  
 @SubTotal money,  
 @IGV money,  
 @Total money,  
 @Estado char(10),  
 @CodEmp varchar(2),  
 @MontoLetras VARCHAR(100)
  
as  
  
  
--DECLARE @Nro_Doc char(8)  
  
BEGIN TRANSACTION  
  
   
  IF @Tipo_documento='NOTA DE VENTA'  
  BEGIN  
  SELECT @Nro_Doc=@Pedido_ID  
  IF (SELECT COUNT(*)  
     FROM Nota_Venta  
     WHERE Nota_Venta_ID=@Venta_id   
                     AND Pedido_ID=@Pedido_ID)<>0  
       
   BEGIN  
   update Nota_Venta set  
     Cliente_ID=@Cliente_ID,Fecha=@Fecha_Pedido,CondPago=@CondPago,  
     FormaPago=@FormaPago,Total=@Total,CodEmp=@CodEmp  
   where Nota_Venta_ID=@Venta_id   
                          AND Pedido_ID=@Pedido_ID   
  
    END  
   END  
   ELSE  
  IF @Tipo_documento='BOLETA DE VENTA'  
  BEGIN  
  
  IF (SELECT COUNT(*)  
     FROM Boleta  
     WHERE Boleta_ID=@Venta_id   
                     AND Pedido_ID=@Pedido_ID)<>0  
  
     BEGIN  
    
   update dbo.Boleta set  
     NroSerie=@NroSerie,NroDocB=@Nro_Doc,Cliente_ID=@Cliente_ID,FechaEmi=@Fecha_Pedido,FechaVen=@Fecha_Pedido,  
     ConPago=@CondPago,FormaPago=@FormaPago,SubTotal=@SubTotal,Total=@Total,  
     MontoLetras=@MontoLetras,CodEmp=@CodEmp  
   where Boleta_ID=@Venta_id   
                          AND Pedido_ID=@Pedido_ID  
  
    END  
   END  
  ELSE  
  IF @Tipo_documento='FACTURA'  
  BEGIN  
  
  --SELECT @Nro_Doc=(select '0'+replicate('0',7-(len(isnull(cast(right(max(NroDocF),8) as int),0)+1)+1))  
  --+ cast(isnull(cast(right(max(NroDocF),6)as int),0)+1 as char(8))from dbo.Factura  
  --where NroSerie=@NroSerie and len(NroDocF)>=7  
  ----and Pedido_ID=(select max(Pedido_ID)from dbo.Factura WHERE NroSerie=@NroSerie)  
  --)  
  
  IF (SELECT COUNT(*)  
     FROM factura  
     WHERE Factura_ID=@Venta_id   
                     AND Pedido_ID=@Pedido_ID )<>0  
   BEGIN  
   update dbo.factura set  
     NroSerie=@NroSerie,NroDocF=@Nro_Doc,Cliente_ID=@Cliente_ID,FechaEmi=@Fecha_Pedido,FechaVen=@Fecha_Pedido,  
     ConPago=@CondPago,FormaPago=@FormaPago,SubTotal=@SubTotal,Total=@Total,IGV=@IGV,  
     MontoLetras=@MontoLetras,CodEmp=@CodEmp  
   where Factura_ID=@Venta_id   
                          AND Pedido_ID=@Pedido_ID  
  
    END  
   END  
    
  IF (SELECT COUNT(*)  
     FROM Cabecera_Ventas  
     WHERE Pedido_ID=@Pedido_ID   
                     AND Venta_id=@Venta_id)<>0  
     BEGIN  
   update dbo.Cabecera_Ventas set  
     NroSerie=@NroSerie,Nro_Documento=@Nro_Doc,Cliente_ID=@Cliente_ID,RazonSocial=@RazonSocial,  
                                        Fecha_Pedido=@Fecha_Pedido,CondPago=@CondPago,FormaPago=@FormaPago,SubTotal=@SubTotal,  
                                        IGV=@IGV,Total=@Total,MontoLetras=@MontoLetras,CodEmp=@CodEmp  
  
   where Pedido_ID=@Pedido_ID  
   
  END  
  
  
 IF @@ERROR<>0  
     BEGIN  
   ROLLBACK TRANSACTION  
   RETURN -1  
   END  
    ELSE  
    BEGIN   
   COMMIT TRANSACTION  
   RETURN 0  
   END  

GO
CREATE PROCEDURE [dbo].[sp_Reporte_TicketCierreCaja]  
    @CodEmp           VARCHAR(2),  
    @idAperturaCierre INT  
AS  
BEGIN  
    SET NOCOUNT ON;  
    SELECT EmpresaRazon     = RTRIM(ISNULL(e.RazonSocial, '')),  
           EmpresaComercial = RTRIM(ISNULL(e.RazonComercial, '')),  
           EmpresaRuc       = RTRIM(ISNULL(e.Ruc, '')),  
           EmpresaDireccion = RTRIM(ISNULL(e.Direccion, '')),  
           EmpresaTelefono  = RTRIM(ISNULL(e.Telefono, '')),  
           idAperturaCierre = acc.idAperturaCierre,  
           Turno            = RTRIM(ISNULL(acc.Turno, '')),  
           FechaTurno       = acc.FechaTurno,  
           FechaCaja        = acc.FechaCaja,  
           HoraIni          = RTRIM(ISNULL(acc.HoraIni, '')),  
           HoraCierre       = RTRIM(ISNULL(acc.HoraCierre, '')),  
           EstadoCaja       = CASE RTRIM(acc.EstadoCaja) WHEN 'C' THEN 'CERRADA' WHEN 'A' THEN 'ABIERTA'  
                                   ELSE RTRIM(ISNULL(acc.EstadoCaja, '')) END,  
           Responsable      = RTRIM(ISNULL(pr.Nombres, '')),  
           Cajero           = RTRIM(ISNULL(pu.Nombres, '')),  
           MontoInicial     = CAST(ISNULL(acc.MontoInicial, 0)   AS DECIMAL(12,2)),  
           VentaDia         = CAST(ISNULL(acc.VentaDia, 0)       AS DECIMAL(12,2)),  
           VentaContado     = CAST(ISNULL(acc.VentaContado, 0)   AS DECIMAL(12,2)),  
           VentaCredito     = CAST(ISNULL(acc.VentaCredito, 0)   AS DECIMAL(12,2)),  
           VentaEfectivo    = CAST(ISNULL(acc.VentaEfectivo, 0)  AS DECIMAL(12,2)),  
           VentaDeposito    = CAST(ISNULL(acc.VentaDeposito, 0)  AS DECIMAL(12,2)),  
           TotalPagos       = CAST(ISNULL(acc.TotalPagos, 0)     AS DECIMAL(12,2)),  
           TotalGastos      = CAST(ISNULL(acc.TotalGastos, 0)    AS DECIMAL(12,2)),  
           TotalIngEfectivo    = CAST(ISNULL(acc.TotalIngEfectivo, 0)  AS DECIMAL(12,2)),  
           EfectivoActual   = CAST(ISNULL(acc.EfectivoActual, 0) AS DECIMAL(12,2))  
      FROM dbo.AperturaCierreCaja acc  
      LEFT JOIN dbo.Empresa e    ON e.Codigo_Emp = acc.CodEmp  
      LEFT JOIN dbo.Personal pr  ON pr.Personal_ID = acc.CodResp  
      LEFT JOIN dbo.Personal pu  ON pu.Personal_ID = acc.UsuarioMod  
     WHERE acc.CodEmp = @CodEmp  
       AND acc.idAperturaCierre = @idAperturaCierre;  
END  
  
GO
--SP_HELPTEXT sp_Reporte_TicketNotaCredito
--GO

--SP_HELPTEXT sp_QuitaDetalleVentas
--GO
 */
public class _Procedimientos5 {
    
}
