/* ================================================================================================
   PAGO DE LA MEMBRESIA: se pasa de sp_Insert_CabeceraVentas a sp_Insert_Detalle_Ventas
   BDGimnasio - 23/11/2026

   Antes: sp_Insert_CabeceraVentas marcaba Efectiva la suscripcion Pendiente del CLIENTE en
   cualquier venta (aunque no se vendiera la membresia) y fallaba si el socio tenia dos pendientes.
   Ahora: sp_Insert_Detalle_Ventas la marca solo cuando la linea vendida es el plan (CodigoM).

   - No cambia ningun parametro: el WinForm SisdataGimnasio (frm_Facturacion) y la web
     (Punto de Venta) siguen llamando igual a los dos procedimientos.
   - Ejecutar las dos partes juntas (F5). Antes, guarde una copia de los dos procedimientos actuales
     (clic derecho -> Generar script como -> ALTER) por si necesita volver atras.
   - Parte 2 se basa en sp_Insert_Detalle_Ventas de ActualizacionGimnasio8_Ventas.sql (05/11/2026).
     Si la version del servidor tiene otros cambios, compare antes de ejecutar.
   ================================================================================================ */

-- ===================== 1. sp_Insert_CabeceraVentas (sin el bloque de la suscripcion) =====================

ALTER PROCEDURE [dbo].[sp_Insert_CabeceraVentas]      
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
GO

-- ===================== 2. sp_Insert_Detalle_Ventas (marca la membresia vendida) =====================
ALTER PROCEDURE [dbo].[sp_Insert_Detalle_Ventas]
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
SET NOCOUNT ON
SET XACT_ABORT ON

DECLARE @CantAnterior real = 0
DECLARE @Diferencia real

BEGIN TRANSACTION

 IF EXISTS(SELECT 1 FROM dbo.Cabecera_Ventas WHERE Pedido_ID=@NroPedido)
 BEGIN

  SELECT @CantAnterior=ISNULL(Cantidad,0)
  FROM Detalle_Ventas
  WHERE Pedido_ID=@NroPedido AND Producto_ID=@Producto_ID

  IF @@ROWCOUNT=0
  BEGIN
   SET @CantAnterior=0
   INSERT INTO Detalle_Ventas
     (Pedido_ID,Producto_ID,Descripcion,PrecioCosto,PrecioUnitario,Cantidad,Importe)
   VALUES(@NroPedido,@Producto_ID,@Descripcion,@PrecioCosto,@PrecioUnitario,@Cantidad,@Importe)
  END
  ELSE
   UPDATE Detalle_Ventas
   SET Descripcion=@Descripcion,PrecioCosto=@PrecioCosto,PrecioUnitario=@PrecioUnitario,
       Cantidad=@Cantidad,Importe=@Importe
   WHERE Pedido_ID=@NroPedido AND Producto_ID=@Producto_ID

  IF @TipoDocumento='NOTA DE VENTA'
  BEGIN
   IF NOT EXISTS(SELECT 1 FROM Detalle_Nota_Venta WHERE Producto_ID=@Producto_ID AND Nota_Venta_ID=@Venta_ID)
    INSERT INTO dbo.Detalle_Nota_Venta
    VALUES(@Venta_ID,@Producto_ID,@Descripcion,@PrecioUnitario,@Cantidad,@Importe)
   ELSE
    UPDATE Detalle_Nota_Venta
    SET Descripcion=@Descripcion,PrecioUnitario=@PrecioUnitario,Cantidad=@Cantidad,Importe=@Importe
    WHERE Producto_ID=@Producto_ID AND Nota_Venta_ID=@Venta_ID
  END
  ELSE IF @TipoDocumento='BOLETA DE VENTA'
  BEGIN
   IF NOT EXISTS(SELECT 1 FROM Detalle_Boleta WHERE Producto_ID=@Producto_ID AND Boleta_ID=@Venta_ID)
    INSERT INTO dbo.Detalle_Boleta
    VALUES(@Venta_ID,@Producto_ID,@Descripcion,@PrecioUnitario,@Cantidad,@Importe)
   ELSE
    UPDATE Detalle_Boleta
    SET Descripcion=@Descripcion,PrecioUnitario=@PrecioUnitario,Cantidad=@Cantidad,Importe=@Importe
    WHERE Producto_ID=@Producto_ID AND Boleta_ID=@Venta_ID
  END
  ELSE IF @TipoDocumento='FACTURA'
  BEGIN
   IF NOT EXISTS(SELECT 1 FROM Detalle_Factura WHERE Producto_ID=@Producto_ID AND Factura_ID=@Venta_ID)
    INSERT INTO dbo.Detalle_Factura
    VALUES(@Venta_ID,@Producto_ID,@Descripcion,@PrecioUnitario,@Cantidad,@Importe)
   ELSE
    UPDATE Detalle_Factura
    SET Descripcion=@Descripcion,PrecioUnitario=@PrecioUnitario,Cantidad=@Cantidad,Importe=@Importe
    WHERE Producto_ID=@Producto_ID AND Factura_ID=@Venta_ID
  END

  -- Solo se descuenta lo que cambio (al grabar por primera vez, la cantidad completa)
  SET @Diferencia=@Cantidad-@CantAnterior

  IF @Diferencia<>0
  BEGIN
   UPDATE dbo.Productos SET StockFinal=(StockFinal-@Diferencia)
   WHERE Producto_ID=@Producto_ID

   UPDATE dbo.TempMovimientoDia_Almacen
   SET CantSalida=(CantSalida+@Diferencia),SaldoFinal=(SaldoFinal-@Diferencia)
   WHERE CodEmp=@CodEmp
     AND FechaMovimiento=@FechaTurno
     AND Turno=@Turno
     AND Producto_ID=@Producto_ID
  END

  /* ------------------------------------------------------------------------------------------
     MEMBRESIA: pago de la suscripcion (antes estaba en sp_Insert_CabeceraVentas).
     Solo cuando la linea vendida ES el plan (Producto_ID = CodigoM): vender otro producto al
     socio ya no deja pagada su suscripcion pendiente.
     - Si el codigo existe en Productos es un producto normal: no se busca suscripcion (una
       busqueda por clave primaria; las ventas de productos no tocan suscripciones_Membresia).
     - Se marca UNA sola suscripcion: la pendiente ACTIVA mas antigua de ese plan y de ese
       cliente (TOP 1), asi nunca falla con "Subquery returned more than 1 value".
     - Al volver a grabar el detalle (modificar venta) la suscripcion ya esta Efectiva y no
       cambia nada.
     ------------------------------------------------------------------------------------------ */
  IF NOT EXISTS(SELECT 1 FROM dbo.Productos WHERE Producto_ID=@Producto_ID)
  BEGIN
   UPDATE s SET s.Estado='Efectiva'
   FROM dbo.suscripciones_Membresia s
   WHERE s.NroSuscripcion_id=(
         SELECT TOP 1 o.NroSuscripcion_id
         FROM dbo.suscripciones_Membresia o
         INNER JOIN dbo.Cabecera_Ventas c ON c.Pedido_ID=@NroPedido AND c.Cliente_ID=o.cliente_id
         WHERE o.CodigoM=RTRIM(@Producto_ID)
           AND o.CodEmp=@CodEmp
           AND o.Condicion='ACTIVA'
           AND o.Estado='Pendiente'
         ORDER BY o.FechaInicio, o.NroSuscripcion_id)
  END
 END

COMMIT TRANSACTION
GO
