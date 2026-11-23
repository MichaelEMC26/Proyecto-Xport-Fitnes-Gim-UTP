/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dto;

/**
 *
/* ---------- Ventas por dias (Cabecera_Ventas) ---------- 
SET NOCOUNT ON;
GO

CREATE OR ALTER PROCEDURE dbo.sp_Consulta_VentasXDias
    @CodEmp varchar(2), @Desde date, @Hasta date, @Filtro varchar(100) = ''
AS
BEGIN
    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));
    SELECT Fecha       = CONVERT(date, v.Fecha_Pedido),
           Hora        = CASE WHEN CAST(v.FechaHora AS time) = '00:00:00' THEN ''
                              ELSE CONVERT(varchar(5), v.FechaHora, 108) END,
           Pedido      = RTRIM(v.Pedido_ID),
           Documento   = RTRIM(ISNULL(v.Tipo_documento, '')),
           Comprobante = RTRIM(ISNULL(v.NroSerie, '')) + '-' + RTRIM(ISNULL(v.Nro_Documento, '')),
           Cliente     = RTRIM(ISNULL(v.RazonSocial, '')),
           Condicion   = RTRIM(ISNULL(v.CondPago, '')),
           FormaPago   = RTRIM(ISNULL(v.FormaPago, '')),
           Turno       = RTRIM(ISNULL(v.Turno, '')),
           Vendedor    = LTRIM(RTRIM(ISNULL(p.Nombres, '') + ' ' + ISNULL(p.ApellidoPaterno, ''))),
           SubTotal    = CAST(ISNULL(v.SubTotal, 0) AS decimal(12,2)),
           IGV         = CAST(ISNULL(v.IGV, 0) AS decimal(12,2)),
           Total       = CAST(ISNULL(v.Total, 0) AS decimal(12,2)),
           Estado      = RTRIM(ISNULL(v.Estado, ''))
      FROM dbo.Cabecera_Ventas v
      LEFT JOIN dbo.Personal p ON p.Personal_ID = v.CodPer
     WHERE v.CodEmp = @CodEmp
       AND v.Fecha_Pedido >= @Desde AND v.Fecha_Pedido < DATEADD(DAY, 1, @Hasta)
       AND (@Filtro = ''
            OR v.RazonSocial LIKE '%' + @Filtro + '%'
            OR v.Cliente_ID LIKE '%' + @Filtro + '%'
            OR v.Pedido_ID LIKE '%' + @Filtro + '%'
            OR RTRIM(ISNULL(v.NroSerie, '')) + '-' + RTRIM(ISNULL(v.Nro_Documento, '')) LIKE '%' + @Filtro + '%')
     ORDER BY v.Fecha_Pedido, v.FechaHora, v.Pedido_ID;
END
GO

/* ---------- Ventas por vendedor (Cabecera_Ventas) ---------- 
CREATE OR ALTER PROCEDURE dbo.sp_Consulta_VentasXVendedor
    @CodEmp varchar(2), @Desde date, @Hasta date, @Filtro varchar(100) = ''
AS
BEGIN
    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));
    SELECT Vendedor    = LTRIM(RTRIM(ISNULL(p.Nombres, '') + ' ' + ISNULL(p.ApellidoPaterno, ''))),
           Fecha       = CONVERT(date, v.Fecha_Pedido),
           Pedido      = RTRIM(v.Pedido_ID),
           Documento   = RTRIM(ISNULL(v.Tipo_documento, '')),
           Comprobante = RTRIM(ISNULL(v.NroSerie, '')) + '-' + RTRIM(ISNULL(v.Nro_Documento, '')),
           Cliente     = RTRIM(ISNULL(v.RazonSocial, '')),
           Condicion   = RTRIM(ISNULL(v.CondPago, '')),
           FormaPago   = RTRIM(ISNULL(v.FormaPago, '')),
           Turno       = RTRIM(ISNULL(v.Turno, '')),
           Total       = CAST(ISNULL(v.Total, 0) AS decimal(12,2)),
           Estado      = RTRIM(ISNULL(v.Estado, ''))
      FROM dbo.Cabecera_Ventas v
      LEFT JOIN dbo.Personal p ON p.Personal_ID = v.CodPer
     WHERE v.CodEmp = @CodEmp
       AND v.Fecha_Pedido >= @Desde AND v.Fecha_Pedido < DATEADD(DAY, 1, @Hasta)
       AND (@Filtro = ''
            OR ISNULL(p.Nombres, '') + ' ' + ISNULL(p.ApellidoPaterno, '') LIKE '%' + @Filtro + '%'
            OR v.RazonSocial LIKE '%' + @Filtro + '%')
     ORDER BY Vendedor, v.Fecha_Pedido, v.Pedido_ID;
END
GO

/* ---------- Turnos por dias (AperturaCierreCaja) ---------- 
CREATE OR ALTER PROCEDURE dbo.sp_Consulta_TurnosXDias
    @CodEmp varchar(2), @Desde date, @Hasta date, @Filtro varchar(100) = ''
AS
BEGIN
    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));
    SELECT Nro            = a.idAperturaCierre,
           FechaTurno     = CONVERT(date, a.FechaTurno),
           Turno          = RTRIM(ISNULL(a.Turno, '')),
           Apertura       = RTRIM(ISNULL(a.HoraIni, '')),
           Cierre         = RTRIM(ISNULL(a.HoraCierre, '')),
           Responsable    = LTRIM(RTRIM(ISNULL(p.Nombres, '') + ' ' + ISNULL(p.ApellidoPaterno, ''))),
           MontoInicial   = CAST(ISNULL(a.MontoInicial, 0) AS decimal(12,2)),
           VentaDia       = CAST(ISNULL(a.VentaDia, 0) AS decimal(12,2)),
           Contado        = CAST(ISNULL(a.VentaContado, 0) AS decimal(12,2)),
           Credito        = CAST(ISNULL(a.VentaCredito, 0) AS decimal(12,2)),
           Efectivo       = CAST(ISNULL(a.VentaEfectivo, 0) AS decimal(12,2)),
           Deposito       = CAST(ISNULL(a.VentaDeposito, 0) AS decimal(12,2)),
           Cobros         = CAST(ISNULL(a.TotalPagos, 0) AS decimal(12,2)),
           Gastos         = CAST(ISNULL(a.TotalGastos, 0) AS decimal(12,2)),
           EfectivoActual = CAST(ISNULL(a.EfectivoActual, 0) AS decimal(12,2)),
           Estado         = CASE RTRIM(a.EstadoCaja) WHEN 'C' THEN 'CERRADA' WHEN 'A' THEN 'ABIERTA'
                                 ELSE RTRIM(ISNULL(a.EstadoCaja, '')) END
      FROM dbo.AperturaCierreCaja a
      LEFT JOIN dbo.Personal p ON p.Personal_ID = a.CodResp
     WHERE a.CodEmp = @CodEmp
       AND a.FechaTurno >= @Desde AND a.FechaTurno < DATEADD(DAY, 1, @Hasta)
       AND (@Filtro = ''
            OR ISNULL(p.Nombres, '') + ' ' + ISNULL(p.ApellidoPaterno, '') LIKE '%' + @Filtro + '%'
            OR a.Turno LIKE '%' + @Filtro + '%')
     ORDER BY a.FechaTurno, a.Turno, a.idAperturaCierre;
END
GO

/* ---------- Historial de membresias (RegistroPeriodoClases + Membresia) ---------- 
CREATE OR ALTER PROCEDURE dbo.sp_Consulta_HistorialMembresias
    @CodEmp varchar(2), @Desde date, @Hasta date, @Filtro varchar(100) = ''
AS
BEGIN
    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));
    SELECT Registro    = RTRIM(r.NroRegistro),
           Pedido      = RTRIM(r.Pedido_ID),
           Documento   = RTRIM(ISNULL(r.Cliente_ID, '')),
           Cliente     = RTRIM(ISNULL(c.RazonSocial, '')),
           Membresia   = RTRIM(ISNULL(m.NombreMembresia, r.CodigoM)),
           FechaInicio = CONVERT(date, r.FechaInicio),
           FechaFin    = CONVERT(date, r.FechaFin),
           Dias        = DATEDIFF(DAY, r.FechaInicio, r.FechaFin) + 1,
           Vigente     = CASE WHEN CONVERT(date, GETDATE()) BETWEEN CONVERT(date, r.FechaInicio)
                                                                 AND CONVERT(date, r.FechaFin)
                              THEN 'SI' ELSE 'NO' END,
           Total       = CAST(ISNULL(r.Total, 0) AS decimal(12,2)),
           Condicion   = RTRIM(ISNULL(r.Condicion, '')),
           Vendedor    = LTRIM(RTRIM(ISNULL(p.Nombres, '') + ' ' + ISNULL(p.ApellidoPaterno, ''))),
           Estado      = RTRIM(ISNULL(r.Estado, ''))
      FROM dbo.RegistroPeriodoClases r
      LEFT JOIN dbo.Membresia m ON m.CodigoM = r.CodigoM
      LEFT JOIN dbo.Cliente c   ON c.Cliente_ID = r.Cliente_ID
      LEFT JOIN dbo.Personal p  ON p.Personal_ID = r.PersonalID
     WHERE (r.CodEmp = @CodEmp OR r.CodEmp IS NULL)
       AND r.FechaInicio >= @Desde AND r.FechaInicio < DATEADD(DAY, 1, @Hasta)
       AND (@Filtro = ''
            OR c.RazonSocial LIKE '%' + @Filtro + '%'
            OR r.Cliente_ID LIKE '%' + @Filtro + '%'
            OR m.NombreMembresia LIKE '%' + @Filtro + '%'
            OR r.NroRegistro LIKE '%' + @Filtro + '%')
     ORDER BY r.FechaInicio, r.NroRegistro;
END
GO

/* ---------- Historial de asistencias (DetRegistroPeriodoClases de RegistroPeriodoClases) ---------- 
CREATE OR ALTER PROCEDURE dbo.sp_Consulta_HistorialAsistencias
    @CodEmp varchar(2), @Desde date, @Hasta date, @Filtro varchar(100) = ''
AS
BEGIN
    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));
    SELECT Fecha       = CONVERT(date, d.FechaIngreso),
           Hora        = CONVERT(varchar(5), d.HoraIngreso, 108),
           Asistencia  = d.IDSecuencia,
           Documento   = RTRIM(d.Cliente_ID),
           Cliente     = RTRIM(ISNULL(c.RazonSocial, '')),
           Membresia   = RTRIM(ISNULL(m.NombreMembresia, ISNULL(r.CodigoM, ''))),
           Registro    = RTRIM(d.NroRegistro),
           FechaInicio = CONVERT(date, r.FechaInicio),
           FechaFin    = CONVERT(date, r.FechaFin)
      FROM dbo.DetRegistroPeriodoClases d
      OUTER APPLY (SELECT TOP 1 x.CodigoM, x.FechaInicio, x.FechaFin, x.CodEmp
                     FROM dbo.RegistroPeriodoClases x
                    WHERE x.NroRegistro = d.NroRegistro
                    ORDER BY x.FechaInicio DESC) r
      LEFT JOIN dbo.Membresia m ON m.CodigoM = r.CodigoM
      LEFT JOIN dbo.Cliente c   ON c.Cliente_ID = d.Cliente_ID
     WHERE (r.CodEmp = @CodEmp OR r.CodEmp IS NULL)
       AND d.FechaIngreso >= @Desde AND d.FechaIngreso < DATEADD(DAY, 1, @Hasta)
       AND (@Filtro = ''
            OR c.RazonSocial LIKE '%' + @Filtro + '%'
            OR d.Cliente_ID LIKE '%' + @Filtro + '%'
            OR m.NombreMembresia LIKE '%' + @Filtro + '%')
     ORDER BY d.FechaIngreso, d.HoraIngreso;
END
GO

/* ---------- Historial de venta de productos (Detalle_Ventas + Productos) ---------- 
CREATE OR ALTER PROCEDURE dbo.sp_Consulta_HistorialVentaProductos
    @CodEmp varchar(2), @Desde date, @Hasta date, @Filtro varchar(100) = ''
AS
BEGIN
    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));
    SELECT Fecha       = CONVERT(date, v.Fecha_Pedido),
           Comprobante = RTRIM(ISNULL(v.NroSerie, '')) + '-' + RTRIM(ISNULL(v.Nro_Documento, '')),
           Cliente     = RTRIM(ISNULL(v.RazonSocial, '')),
           Codigo      = RTRIM(d.Producto_ID),
           Producto    = RTRIM(COALESCE(pr.NombreProducto, d.Descripcion, '')),
           Cantidad    = CAST(ISNULL(d.Cantidad, 0) AS decimal(12,2)),
           Precio      = CAST(ISNULL(d.PrecioUnitario, 0) AS decimal(12,2)),
           Importe     = CAST(ISNULL(d.Importe, 0) AS decimal(12,2)),
           Estado      = RTRIM(ISNULL(v.Estado, ''))
      FROM dbo.Detalle_Ventas d
      INNER JOIN dbo.Cabecera_Ventas v ON v.Pedido_ID = d.Pedido_ID
      OUTER APPLY (SELECT TOP 1 p.NombreProducto
                     FROM dbo.Productos p
                    WHERE p.Producto_ID = d.Producto_ID
                      AND (p.CodEmp = @CodEmp OR p.CodEmp IS NULL)) pr
     WHERE v.CodEmp = @CodEmp
       AND v.Fecha_Pedido >= @Desde AND v.Fecha_Pedido < DATEADD(DAY, 1, @Hasta)
       AND (@Filtro = ''
            OR d.Producto_ID LIKE '%' + @Filtro + '%'
            OR pr.NombreProducto LIKE '%' + @Filtro + '%'
            OR d.Descripcion LIKE '%' + @Filtro + '%'
            OR v.RazonSocial LIKE '%' + @Filtro + '%')
     ORDER BY v.Fecha_Pedido, v.Pedido_ID, Producto;
END
GO

/* ---------- Historial de compras de mercaderia (Detalle_Registro_compra + Productos) ---------- 
CREATE OR ALTER PROCEDURE dbo.sp_Consulta_HistorialComprasMercaderia
    @CodEmp varchar(2), @Desde date, @Hasta date, @Filtro varchar(100) = ''
AS
BEGIN
    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));
    SELECT Fecha       = CONVERT(date, r.FechaIngreso),
           Registro    = RTRIM(r.Registro_Compra_ID),
           Proveedor   = RTRIM(ISNULL(r.Proveedor, '')),
           Documento   = LTRIM(RTRIM(ISNULL(r.TipoDoc, '') + ' ' + RTRIM(ISNULL(r.NroSerie, ''))
                         + CASE WHEN ISNULL(r.NroSerie, '') = '' THEN '' ELSE '-' END + RTRIM(ISNULL(r.NroDocumento, '')))),
           Codigo      = RTRIM(d.Producto_ID),
           Producto    = RTRIM(ISNULL(pr.NombreProducto, '')),
           Cantidad    = CAST(ISNULL(d.Cantidad, 0) AS decimal(12,2)),
           PrecioCosto = CAST(ISNULL(d.PrecioCosto, 0) AS decimal(12,2)),
           Importe     = CAST(ISNULL(d.Importe, 0) AS decimal(12,2))
      FROM dbo.Detalle_Registro_compra d
      INNER JOIN dbo.Registro_de_Compra r ON r.Registro_Compra_ID = d.Registro_Compra_ID
      OUTER APPLY (SELECT TOP 1 p.NombreProducto
                     FROM dbo.Productos p
                    WHERE p.Producto_ID = d.Producto_ID
                      AND (p.CodEmp = @CodEmp OR p.CodEmp IS NULL)) pr
     WHERE r.CodEmp = @CodEmp
       AND r.FechaIngreso >= @Desde AND r.FechaIngreso < DATEADD(DAY, 1, @Hasta)
       AND (@Filtro = ''
            OR d.Producto_ID LIKE '%' + @Filtro + '%'
            OR pr.NombreProducto LIKE '%' + @Filtro + '%'
            OR r.Proveedor LIKE '%' + @Filtro + '%'
            OR r.NroDocumento LIKE '%' + @Filtro + '%')
     ORDER BY r.FechaIngreso, r.Registro_Compra_ID, Producto;
END
GO

/* ---------- Historial de cobranzas a clientes (DetalleCtasXCobrar de CtasXCobrar) ---------- 
CREATE OR ALTER PROCEDURE dbo.sp_Consulta_HistorialCobranzasClientes
    @CodEmp varchar(2), @Desde date, @Hasta date, @Filtro varchar(100) = ''
AS
BEGIN
    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));
    SELECT Fecha     = CONVERT(date, d.FechaAmortizacion),
           Cuenta    = RTRIM(c.Cobro_ID),
           Pago      = d.Codigo,
           Documento = RTRIM(c.Cliente_ID),
           Cliente   = RTRIM(ISNULL(cl.RazonSocial, '')),
           Venta     = LTRIM(RTRIM(ISNULL(c.TipoDocumento, '') + ' ' + ISNULL(c.NroDocumento, ''))),
           Deuda     = CAST(ISNULL(d.MontoDeuda, 0) AS decimal(12,2)),
           Cobrado   = CAST(ISNULL(d.ACuenta, 0) AS decimal(12,2)),
           Saldo     = CAST(ISNULL(d.Saldo, 0) AS decimal(12,2)),
           Usuario   = LTRIM(RTRIM(ISNULL(p.Nombres, '') + ' ' + ISNULL(p.ApellidoPaterno, ''))),
           Estado    = RTRIM(ISNULL(c.CondicionCredito, ''))
      FROM dbo.DetalleCtasXCobrar d
      INNER JOIN dbo.CtasXCobrar c ON c.Cobro_ID = d.Cobro_ID
      LEFT JOIN dbo.Cliente cl     ON cl.Cliente_ID = c.Cliente_ID
      LEFT JOIN dbo.Personal p     ON p.Personal_ID = d.UsuarioMod
     WHERE (c.CodEmp = @CodEmp OR c.CodEmp IS NULL)
       AND d.FechaAmortizacion >= @Desde AND d.FechaAmortizacion < DATEADD(DAY, 1, @Hasta)
       AND (@Filtro = ''
            OR cl.RazonSocial LIKE '%' + @Filtro + '%'
            OR c.Cliente_ID LIKE '%' + @Filtro + '%'
            OR c.Cobro_ID LIKE '%' + @Filtro + '%')
     ORDER BY d.FechaAmortizacion, c.Cobro_ID, d.Codigo;
END
GO

/* ---------- Historial de pagos a proveedores (DetalleCtasXPagar de CtasXPagar) ---------- 
CREATE OR ALTER PROCEDURE dbo.sp_Consulta_HistorialPagosProveedores
    @CodEmp varchar(2), @Desde date, @Hasta date, @Filtro varchar(100) = ''
AS
BEGIN
    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));
    SELECT Fecha     = CONVERT(date, d.FechaAmortizacion),
           Cuenta    = RTRIM(c.Pago_ID),
           Pago      = d.Codigo,
           Ruc       = RTRIM(ISNULL(pr.Ruc, '')),
           Proveedor = RTRIM(ISNULL(pr.RazonSocial, c.Proveedor_ID)),
           Documento = LTRIM(RTRIM(ISNULL(c.TipoDocumento, '') + ' ' + ISNULL(c.NroDocumento, ''))),
           Deuda     = CAST(ISNULL(d.MontoDeuda, 0) AS decimal(12,2)),
           Pagado    = CAST(ISNULL(d.ACuenta, 0) AS decimal(12,2)),
           Saldo     = CAST(ISNULL(d.Saldo, 0) AS decimal(12,2)),
           Usuario   = LTRIM(RTRIM(ISNULL(p.Nombres, '') + ' ' + ISNULL(p.ApellidoPaterno, ''))),
           Estado    = RTRIM(ISNULL(c.CondicionCredito, ''))
      FROM dbo.DetalleCtasXPagar d
      INNER JOIN dbo.CtasXPagar c ON c.Pago_ID = d.Pago_ID
      LEFT JOIN dbo.Proveedor pr  ON CONVERT(varchar(20), pr.Proveedor_ID) = c.Proveedor_ID
      LEFT JOIN dbo.Personal p    ON p.Personal_ID = d.UsuarioMod
     WHERE (c.CodEmp = @CodEmp OR c.CodEmp IS NULL)
       AND d.FechaAmortizacion >= @Desde AND d.FechaAmortizacion < DATEADD(DAY, 1, @Hasta)
       AND (@Filtro = ''
            OR pr.RazonSocial LIKE '%' + @Filtro + '%'
            OR pr.Ruc LIKE '%' + @Filtro + '%'
            OR c.NroDocumento LIKE '%' + @Filtro + '%')
     ORDER BY d.FechaAmortizacion, c.Pago_ID, d.Codigo;
END
GO

/* ---------- Historial de gastos diversos (GastosDiversos) ---------- 
CREATE OR ALTER PROCEDURE dbo.sp_Consulta_HistorialGastosDiversos
    @CodEmp varchar(2), @Desde date, @Hasta date, @Filtro varchar(100) = ''
AS
BEGIN
    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));
    SELECT Fecha      = CONVERT(date, g.FechaGasto),
           Codigo     = RTRIM(CONVERT(varchar(20), g.CodigoGastos)),
           Concepto   = RTRIM(ISNULL(g.Concepto, '')),
           Referencia = RTRIM(ISNULL(g.GlosaRef, '')),
           Documento  = RTRIM(ISNULL(g.NroDocumento, '')),
           Detalle    = RTRIM(ISNULL(g.DetalleGasto, '')),
           Importe    = CAST(ISNULL(g.ImporteGasto, 0) AS decimal(12,2)),
           Usuario    = LTRIM(RTRIM(ISNULL(p.Nombres, '') + ' ' + ISNULL(p.ApellidoPaterno, '')))
      FROM dbo.GastosDiversos g
      LEFT JOIN dbo.Personal p ON p.Personal_ID = g.PersonalID
     WHERE (g.CodEmp = @CodEmp OR g.CodEmp IS NULL)
       AND g.FechaGasto >= @Desde AND g.FechaGasto < DATEADD(DAY, 1, @Hasta)
       AND (@Filtro = ''
            OR g.Concepto LIKE '%' + @Filtro + '%'
            OR g.GlosaRef LIKE '%' + @Filtro + '%'
            OR g.DetalleGasto LIKE '%' + @Filtro + '%'
            OR g.NroDocumento LIKE '%' + @Filtro + '%')
     ORDER BY g.FechaGasto, g.CodigoGastos;
END

GO

--------------------------------------------MEMBRESIAS ----------------------------------------------------------

--GO
--ALTER TABLE [dbo].[Cliente] ADD codigo_qr_token VARCHAR(255) NULL
--GO

  

ALTER   PROCEDURE [dbo].[sp_Insert_Clientes]    
 @Cliente_ID CHAR(8),    
 @RazonSocial VARCHAR (100),    
 @Direccion VARCHAR (100) ,    
 @Telefono varchar(20),    
 @Estado varchar(10),    
 @Telefono2 varchar(20),    
 @Correo VARCHAR (100),    
 @Sexo varchar(20),    
 @RucDNI varchar(12),    
 @TipoId varchar(10),    
 @Foto varchar(100),    
 @Contacto varchar(100),    
 @TelefonoCont varchar(20),    
 @HuellaDactilar VARBINARY(MAX)--,
 --@codigo_qr_token VARCHAR(255)
as    
 BEGIN TRANSACTION  
   
  IF EXISTS(select 1    
   FROM cliente    
   WHERE Cliente_ID=@Cliente_ID)  
  BEGIN     
    
  UPDATE cliente set RazonSocial=@RazonSocial,Direccion=@Direccion,Telefono=@Telefono,    
         Telefono2=@Telefono2,Correo=@Correo,RucDNI=@RucDNI,TipoId=@TipoId,    
         HuellaDactilar=ISNULL(@HuellaDactilar, HuellaDactilar)    
  WHERE Cliente_ID=@Cliente_ID    
    END  
   ELSE  
  BEGIN  
  INSERT INTO cliente(Cliente_ID,RazonSocial,Direccion,Telefono,Estado,Telefono2,Correo,Sexo,RucDNI,TipoId,    
         Foto,Contacto,TelefonoCont,HuellaDactilar)    
  VALUES(@Cliente_ID,@RazonSocial,@Direccion,@Telefono,@Estado,@Telefono2,@Correo,@Sexo,@RucDNI,@TipoId,    
         @Foto,@Contacto,@TelefonoCont,@HuellaDactilar)    
  END    
  IF @@error <>0    
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

CREATE TABLE [dbo].[Frecuencia](
	[IDFrecuencia] [int] NOT NULL,
	[Frecuencia] [varchar](50) NULL,
	[Estado] [char](10) NULL,
 CONSTRAINT [PK__Frecuencia] PRIMARY KEY CLUSTERED 
(
	[IDFrecuencia] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

ALTER TABLE [dbo].[Membresia]ALTER COLUMN [CodigoM]VARCHAR(4)NOT NULL
GO
ALTER TABLE [Membresia] DROP COLUMN[Frecuencia]
GO
ALTER TABLE [Membresia] DROP COLUMN[UsuarioMod]
GO
ALTER TABLE [Membresia] DROP COLUMN[IDTarifa]
GO
ALTER TABLE [dbo].[Membresia]ADD [IDFrecuencia]INT NULL
GO

INSERT INTO [Frecuencia]VALUES(1,'FULL DIARIO','Activo')
GO
INSERT INTO [Frecuencia]VALUES(2,'INTERDIARIO','Activo')
GO
UPDATE [Membresia] SET [IDFrecuencia]=2                  
GO
DROP TABLE suscripciones_Membresia
GO
-- 3. Historial de Suscripciones (Membresías contratadas)
CREATE TABLE suscripciones_Membresia (
    NroSuscripcion_id CHAR(8)NOT NULL,
    cliente_id CHAR(8)NOT NULL,
    CodigoM VARCHAR(4),
    FechaInicio DATETIME NOT NULL,
    FechaFin DATETIME NOT NULL,
    Personal_ID INT,
	SubTotal MONEY,
    Descuento MONEY,
	Total MONEY,
	DiasClases INT,
    Estado VARCHAR(10),
	Condicion VARCHAR(30) DEFAULT 'ACTIVA', -- Activa, Vencida, Cancelada
	Observacion VARCHAR(255)NULL,
	CodEmp VARCHAR(2)NULL,
    FOREIGN KEY (cliente_id) REFERENCES Cliente(cliente_id),
    FOREIGN KEY (CodigoM) REFERENCES Membresia(CodigoM)
);
GO

ALTER TABLE suscripciones_Membresia DROP COLUMN Estado
GO
ALTER TABLE suscripciones_Membresia DROP COLUMN Condicion
GO
ALTER TABLE suscripciones_Membresia DROP COLUMN Observacion
GO
ALTER TABLE suscripciones_Membresia DROP COLUMN CodEmp
GO
ALTER TABLE [dbo].[suscripciones_Membresia] 
DROP CONSTRAINT [DF__suscripci__Condi__0ADD8CFD];
GO

ALTER TABLE suscripciones_Membresia ADD DiasClases INT
GO
ALTER TABLE suscripciones_Membresia ADD Estado CHAR(10)
GO
ALTER TABLE suscripciones_Membresia ADD Condicion VARCHAR(30)
GO
ALTER TABLE suscripciones_Membresia ADD Observacion VARCHAR(255)NULL
GO
ALTER TABLE suscripciones_Membresia ADD CodEmp VARCHAR(2)
GO

ALTER TABLE suscripciones_Membresia DROP CONSTRAINT PK__suscripc__F9DAC510570ED854
GO
ALTER TABLE suscripciones_Membresia DROP CONSTRAINT FK__suscripci__clien__0BD1B136
GO
ALTER TABLE suscripciones_Membresia DROP CONSTRAINT FK__suscripci__Codig__0CC5D56F
GO
ALTER TABLE suscripciones_Membresia ADD PRIMARY KEY(NroSuscripcion_id)
GO
--ALTER TABLE [dbo].[RegistroPeriodoClases]DROP CONSTRAINT [FK__RegistroP__Perso__546180BB]
--GO
--ALTER TABLE [dbo].[DetRegistroPeriodoClases]DROP CONSTRAINT [FK__DetRegist__NroRe__7775B2CE]
--GO
ALTER TABLE [dbo].[suscripciones_Membresia]DROP CONSTRAINT PK__suscripc__F9DAC510570ED854
GO
ALTER TABLE [dbo].[DetRegistroPeriodoClases]DROP CONSTRAINT PK__DetSuscr__62FC8F58E97724B6
GO
TRUNCATE TABLE [DetRegistroPeriodoClases]
GO

CREATE TABLE [dbo].[DetSuscripcionPeriodoClases2](
	[idRegistro] [int] IDENTITY(1,1) NOT NULL,
	[NroSuscripcion_id] [char](8) NOT NULL,
	[Cliente_ID] [varchar](8) NULL,
	[IDSecuencia] [int] NOT NULL,
	[Rutina] [varchar](20) NULL,
	[Asistio] [char](2) NULL,
PRIMARY KEY CLUSTERED 
(
	[idRegistro] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

--INSERT INTO [DetSuscripcionPeriodoClases2]([NroSuscripcion_id],
--	[Cliente_ID],
--	[IDSecuencia],
--	[Rutina],
--	[Asistio])
--SELECT [NroSuscripcion_id],
--	[Cliente_ID],
--	[IDSecuencia],
--	[Rutina],
--	[Asistio]FROM [DetSuscripcionPeriodoClases]
GO
--DELETE FROM [dbo].[RegistroPeriodoClases]
--GO

INSERT INTO RegistroPeriodoClases(NroRegistro,Pedido_ID,cliente_id,CodigoM,FechaInicio,FechaFin,PersonalID,SubTot,
                                    IGV,Total,Estado,Condicion,Observacion,CodEmp)
SELECT NroSuscripcion_id,'',cliente_id,[CodigoM],[FechaInicio],[FechaFin],Personal_ID,[Total],Descuento,[Total],Estado,
       Condicion,Observacion,CodEmp
FROM suscripciones_Membresia

GO
--DROP TABLE DetRegistroAsistencias
GO
CREATE TABLE DetRegistroAsistencias(
	idRegistro int IDENTITY(1,1) NOT NULL,
	NroSuscripcion_id char(8) NOT NULL,
	Cliente_ID char(8) NULL,
    IDSecuencia INT NULL,
	HoraIngreso datetime NULL,
	FechaIngreso datetime NULL,
	Turno varchar(2) NULL,
	FOREIGN KEY (NroSuscripcion_id) REFERENCES suscripciones_Membresia(NroSuscripcion_id),
	FOREIGN KEY (cliente_id) REFERENCES Cliente(cliente_id)
);
GO
--DELETE FROM suscripciones_Membresia
GO
--ALTER TABLE [dbo].[RegistroPeriodoClases]DROP CONSTRAINT [FK__RegistroP__Codig__5555A4F4]
--GO
UPDATE [RegistroPeriodoClases] SET [CodigoM]='0'+[CodigoM]
GO
UPDATE [dbo].[Membresia]SET [CodigoM]='0'+[CodigoM]
GO
--ALTER TABLE [RegistroPeriodoClases] ADD CONSTRAINT [FK__RegistroP__Codig__5555A4F4]
--REFERENCES [Membresia]([CodigoM])
--GO

INSERT INTO suscripciones_Membresia(NroSuscripcion_id,cliente_id,CodigoM,FechaInicio,FechaFin,Personal_ID,SubTotal,
                                    Descuento,Total,Estado,Condicion,Observacion,CodEmp)
SELECT [NroRegistro],SUBSTRING([Cliente_ID],1,8),[CodigoM],[FechaInicio],[FechaFin],[PersonalID],[Total],0,[Total],'Efectiva',
       'CANCELADA','',[CodEmp]
FROM RegistroPeriodoClases

GO

--------------------------------HORARIOS ---------------------------------------------------

-- 4. Tabla de Instructores
CREATE TABLE instructores (
    instructor_id INT PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    especialidad VARCHAR(50)
);

-- 5. Catálogo de Clases Disponibles
CREATE TABLE clases (
    clase_id INT IDENTITY(1,1)PRIMARY KEY,
    nombre_clase VARCHAR(50) NOT NULL, -- Ej: Spinning, Zumba, Boxeo
    descripcion TEXT
);
GO
-- 6. Programación de Horarios (Sesiones específicas)
CREATE TABLE horarios_clases (
    horario_id INT IDENTITY(1,1)PRIMARY KEY,
    clase_id INT,
    instructor_id INT,
    fecha DATE NOT NULL,
    hora_inicio TIME NOT NULL,
    capacidad_maxima INT NOT NULL,
    FOREIGN KEY (clase_id) REFERENCES clases(clase_id),
    FOREIGN KEY (instructor_id) REFERENCES instructores(instructor_id)
);
GO
-- 7. Registro de Asistencia / Reservas
CREATE TABLE reservas_clases (
    reserva_id INT IDENTITY(1,1)PRIMARY KEY,
    cliente_id CHAR(8),
    horario_id INT,
    fecha_reserva TIMESTAMP ,
    estado_reserva VARCHAR(20) DEFAULT 'Confirmada', -- Confirmada, Cancelada, Asistió
    FOREIGN KEY (Cliente_ID) REFERENCES cliente(cliente_id),
    FOREIGN KEY (horario_id) REFERENCES horarios_clases(horario_id),
    -- Evita que un mismo usuario se inscriba dos veces a la misma sesión exacta
    CONSTRAINT unica_reserva UNIQUE (cliente_id, horario_id) 
);

GO
---------------------------------------------------------------------------------------------

--ALTER TABLE [dbo].[DetRegistroPeriodoClases]  WITH CHECK ADD FOREIGN KEY([NroRegistro])
--REFERENCES [dbo].[RegistroPeriodoClases] ([NroRegistro])
--GO


CREATE proc sp_InsertDetRegistroPeriodoClases      
 @NroRegistro char(10),      
 @Cliente_ID varchar(20),      
 @FechaHora datetime,      
 @FechaFin datetime,  
 @Turno varchar(2)      
      
      
as      
      
BEGIN TRANSACTION      
      
DECLARE @IDSecuencia int      
      
SELECT @IDSecuencia=(SELECT ISNULL(MAX(IDSecuencia),1)FROM DetRegistroPeriodoClases      
                     WHERE NroRegistro=@NroRegistro      
                       and Cliente_ID=@Cliente_ID)      
IF(SELECT COUNT(*)      
   FROM DetRegistroPeriodoClases      
   WHERE NroRegistro=@NroRegistro      
     and Cliente_ID=@Cliente_ID      
     and IDSecuencia=@IDSecuencia)>0      
           
 BEGIN      
        
 SELECT @IDSecuencia=@IDSecuencia+1      
       
  INSERT into dbo.DetRegistroPeriodoClases      
 values(@NroRegistro,@Cliente_ID,@IDSecuencia,@FechaHora,@FechaFin,@Turno)      
              
   END      
 ELSE      
 BEGIN      
       
  INSERT into dbo.DetRegistroPeriodoClases      
 values(@NroRegistro,@Cliente_ID,@IDSecuencia,@FechaHora,@FechaFin,@Turno)      
       
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

SP_DEPENDS DetRegistroPeriodoClases

GO

  
    

------------------------------------------REGISTRAR MEMBRESIAS --------------------------------------------------
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_generasuscripciones_Membresia')
BEGIN
DROP PROC sp_generasuscripciones_Membresia
END
GO
CREATE proc sp_generasuscripciones_Membresia--'01'  
@CodEmp varchar(2)  
as  
  
SELECT RIGHT('00000000' + CAST(ISNULL(CAST(RIGHT(MAX(NroSuscripcion_id), 8) AS INT), 0) + 1 AS VARCHAR(8)), 8) 
FROM dbo.suscripciones_Membresia WHERE CodEmp = @CodEmp;  
  
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_Listasuscripciones_MembresiaXRegistro')
BEGIN
DROP PROC sp_Listasuscripciones_MembresiaXRegistro
END
GO
CREATE proc sp_Listasuscripciones_MembresiaXRegistro--'00000002','40424328'  
 @CodEmp varchar(2),  
 @NroSuscripcion_id char(8)  
  
as  
  
SELECT  NroSuscripcion_id,cliente_id,CodigoM,FechaInicio,FechaFin,Personal_ID,SubTotal,Descuento,
	    Total,Estado,Condicion,Observacion 
FROM dbo.suscripciones_Membresia  
WHERE CodEmp=@CodEmp  
  and NroSuscripcion_id=@NroSuscripcion_id  
      
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_ListaMembresia')
BEGIN
DROP PROC sp_ListaMembresia
END
GO
CREATE proc sp_ListaMembresia    
as    
select CodigoM,NombreMembresia,Duracion_Dias,Duracion_Semanas,Duracion_Meses,Duracion_Anios,  
       DiasLimite,Tarifa,m.IDFrecuencia,f.Frecuencia,m.Estado--,UsuarioMod    
from Membresia m
INNER JOIN Frecuencia f ON m.IDFrecuencia=f.IDFrecuencia
  
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_ListaMembresiaXCod')
BEGIN
DROP PROC sp_ListaMembresiaXCod
END
GO
CREATE proc sp_ListaMembresiaXCod    
@CodigoM varchar(4)    
as    
select CodigoM,NombreMembresia,Duracion_Dias,Duracion_Semanas,Duracion_Meses,Duracion_Anios,  
       DiasLimite,Tarifa,m.IDFrecuencia,f.Frecuencia,m.Estado--,UsuarioMod    
from Membresia m
INNER JOIN Frecuencia f ON m.IDFrecuencia=f.IDFrecuencia 
 AND CodigoM=@CodigoM   
    
  
GO

CREATE TABLE [dbo].[DetSuscripcionPeriodoClases](
	[idRegistro] [int] IDENTITY(1,1) NOT NULL,
	[NroSuscripcion_id] [char](8) NOT NULL,
	[Cliente_ID] [varchar](8) NULL,
	[IDSecuencia] [int] NOT NULL,
	[Rutina] [varchar](20) NULL,
	[Asistio] [char](2) NULL,
PRIMARY KEY CLUSTERED 
(
	[idRegistro] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_InsertSuscripciones_Membresia')
BEGIN
DROP PROC sp_InsertSuscripciones_Membresia
END
GO
CREATE proc sp_InsertSuscripciones_Membresia  
 @NroSuscripcion_id char(8),   
 @Cliente_ID char(8),  
 @CodigoM varchar(4),  
 @FechaInicio datetime,  
 @FechaFin datetime,  
 @PersonalID int,  
 @SubTotal money ,  
 @Descuento money ,  
 @Total money ,  
 @DiasClases INT,
 @Estado char(10) ,  
 @Condicion varchar(30),  
 @Observacion varchar(255),  
 @CodEmp varchar(2)
  
as  
  
BEGIN 

--DECLARE @CantDias INT

SET NOCOUNT ON
SET XACT_ABORT ON
  BEGIN TRY
    BEGIN TRANSACTION

	--SET @CantDias=(SELECT [Duracion_Dias]FROM [dbo].[Membresia]WHERE CodigoM=@CodigoM)

IF EXISTS  
   (SELECT 1 FROM suscripciones_Membresia  
   WHERE NroSuscripcion_id=@NroSuscripcion_id  
     and CodEmp=@CodEmp)  
       
 BEGIN  
 UPDATE [dbo].[suscripciones_Membresia]
    SET Cliente_ID=@Cliente_ID,CodigoM=@CodigoM,FechaInicio=@FechaInicio,FechaFin=@FechaFin,
	    SubTotal=@SubTotal,Descuento=@Descuento,Total=@Total,DiasClases=@DiasClases,Estado=@Estado,Condicion=@Condicion,
		Observacion=@Observacion
 WHERE  NroSuscripcion_id=@NroSuscripcion_id  
    and CodEmp=@CodEmp
	END
   ELSE
  BEGIN
 INSERT into [suscripciones_Membresia]
       (NroSuscripcion_id,Cliente_ID,CodigoM,FechaInicio,FechaFin,Personal_ID,SubTotal,Descuento,  
        Total,DiasClases,Estado,Condicion,Observacion,CodEmp)   
 values(@NroSuscripcion_id,@Cliente_ID,@CodigoM,@FechaInicio,@FechaFin,@PersonalID,@SubTotal,@Descuento,  
        @Total,@DiasClases,@Estado,@Condicion,@Observacion,@CodEmp)  
          
   END  

   DECLARE @IDSecuencia INT = 1;
   DELETE FROM [DetSuscripcionPeriodoClases] WHERE NroSuscripcion_id=@NroSuscripcion_id
   WHILE @IDSecuencia <= @DiasClases
   BEGIN

       INSERT INTO [DetSuscripcionPeriodoClases] (
           [NroSuscripcion_id], 
           [Cliente_ID], 
           [IDSecuencia], 
           [Rutina], 
           [Asistio]
       )
       VALUES (
           @NroSuscripcion_id, 
           @Cliente_ID, 
           @IDSecuencia,
           'CLASE', 
           'NO'
       );

       SET @IDSecuencia += 1;
   END;

   COMMIT TRANSACTION
     RETURN 0
	END TRY
  BEGIN CATCH
  IF @@TRANCOUNT > 0
    BEGIN
	ROLLBACK TRANSACTION
	END
	 RETURN -1
	 END CATCH
   END
	  

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_ListaDetSuscripcionPeriodoClases')
BEGIN
DROP PROC sp_ListaDetSuscripcionPeriodoClases
END
GO
CREATE proc sp_ListaDetSuscripcionPeriodoClases--'00000001','40424341'  
 @NroRegistro char(10),  
 @Cliente_ID varchar(20)  
  
as  
  
SELECT [NroSuscripcion_id],[Cliente_ID],[Rutina],[IDSecuencia],[Asistio] 
FROM dbo.[DetSuscripcionPeriodoClases]  
WHERE NroSuscripcion_id=@NroRegistro  
  and Cliente_ID=@Cliente_ID

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_autogenera_CodClientes')
BEGIN
DROP PROC sp_autogenera_CodClientes
END
GO
CREATE PROCEDURE sp_autogenera_CodClientes   
AS   
 BEGIN     
 SET NOCOUNT ON;      
 SELECT RIGHT('00000000' + CAST(ISNULL(MAX(CAST(RIGHT(Cliente_ID, 8) AS INT)), 0) + 1 AS VARCHAR(8)), 8)      
 FROM Cliente; 
END;  

GO
-- Consulta de Membresias (lista principal). Corrige:  
--  * el segundo UPDATE usaba <= y volvia ACTIVA toda membresia vencida (ahora >).  
--  * faltaba la columna Frecuencia: el formulario lee 17 columnas por posicion.  
--  * no excluia las membresias Anuladas.  
--  * devolvia una segunda tabla "Result" y, si fallaba, solo esa tabla (Referencia a objeto...).  

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_Listasuscripciones_Membresia')
BEGIN
DROP PROC sp_Listasuscripciones_Membresia
END
GO
CREATE   PROCEDURE sp_Listasuscripciones_Membresia  '01',1  
@CodEmp varchar(2),  
@Opc int  
AS  
BEGIN  
SET NOCOUNT ON  
SET XACT_ABORT ON  
  
BEGIN TRANSACTION  
  
UPDATE dbo.suscripciones_Membresia SET Condicion = 'TERMINADA'  
WHERE CAST(FechaFin AS date) <= CAST(GETDATE() AS date)  
  AND CodEmp = @CodEmp  
  AND Estado <> 'Anulada'  
  
UPDATE dbo.suscripciones_Membresia SET Condicion = 'ACTIVA'  
WHERE CAST(FechaFin AS date) > CAST(GETDATE() AS date)  
  AND CodEmp = @CodEmp  
  AND Estado <> 'Anulada'  
  
COMMIT TRANSACTION  
  
IF (@Opc = 1)  
BEGIN  
  SELECT DISTINCT rpc.NroSuscripcion_id, CodSocio = rpc.Cliente_ID, SocioCliente = cli.RazonSocial, rpc.CodigoM,  
         Periodo = m.NombreMembresia, Duracion = DATEDIFF(DAY, rpc.FechaInicio, rpc.FechaFin), F.Frecuencia,  
         rpc.FechaInicio, rpc.FechaFin, rpc.Personal_ID, rpc.SubTotal, rpc.Descuento, rpc.Total,DiasClases, rpc.Estado, rpc.Condicion, rpc.Observacion  
  FROM dbo.suscripciones_Membresia rpc  
  INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
  INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
  INNER JOIN [dbo].[Frecuencia]F ON m.[IDFrecuencia]=F.[IDFrecuencia]
  WHERE rpc.CodEmp = @CodEmp AND rpc.Condicion = 'ACTIVA' AND rpc.Estado <> 'Anulada'  
  UNION  
  SELECT DISTINCT rpc.NroSuscripcion_id, rpc.Cliente_ID, cli.RazonSocial, rpc.CodigoM,  
         m.NombreMembresia, DATEDIFF(DAY, rpc.FechaInicio, rpc.FechaFin), F.Frecuencia,  
         rpc.FechaInicio, rpc.FechaFin, rpc.Personal_ID, rpc.SubTotal, rpc.Descuento, rpc.Total,DiasClases, rpc.Estado, rpc.Condicion, rpc.Observacion  
  FROM dbo.suscripciones_Membresia rpc  
  INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
  INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
  INNER JOIN [dbo].[Frecuencia]F ON m.[IDFrecuencia]=F.[IDFrecuencia]
  WHERE rpc.CodEmp = @CodEmp AND rpc.Condicion = 'TERMINADA' AND rpc.Estado <> 'Anulada'  
    --AND rpc.Pedido_ID IN (SELECT NroRegistro FROM dbo.CtasXCobrar WHERE CodEmp = @CodEmp AND CondicionCredito = 'ADEUDA')  
  ORDER BY 1  
END  
ELSE  
BEGIN  
  SELECT DISTINCT rpc.NroSuscripcion_id, CodSocio = rpc.Cliente_ID, SocioCliente = cli.RazonSocial, rpc.CodigoM,  
         Periodo = m.NombreMembresia, Duracion = DATEDIFF(DAY, rpc.FechaInicio, rpc.FechaFin),F.Frecuencia,  
         rpc.FechaInicio, rpc.FechaFin, rpc.Personal_ID, rpc.SubTotal, rpc.Descuento, rpc.Total,DiasClases, rpc.Estado, rpc.Condicion, rpc.Observacion  
  FROM dbo.suscripciones_Membresia rpc  
  INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
  INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
  INNER JOIN [dbo].[Frecuencia]F ON m.[IDFrecuencia]=F.[IDFrecuencia]
  WHERE rpc.CodEmp = @CodEmp AND rpc.Condicion = 'TERMINADA' AND rpc.Estado <> 'Anulada'  
    --AND rpc.Pedido_ID NOT IN (SELECT NroRegistro FROM dbo.CtasXCobrar WHERE CodEmp = @CodEmp)  
    AND CAST(rpc.FechaFin AS date) <= CAST(GETDATE() AS date)  
  UNION  
  SELECT DISTINCT rpc.NroSuscripcion_id, rpc.Cliente_ID, cli.RazonSocial, rpc.CodigoM,  
         m.NombreMembresia, DATEDIFF(DAY, rpc.FechaInicio, rpc.FechaFin), F.Frecuencia,  
         rpc.FechaInicio, rpc.FechaFin, rpc.Personal_ID, rpc.SubTotal, rpc.Descuento, rpc.Total, DiasClases,rpc.Estado, rpc.Condicion, rpc.Observacion  
  FROM dbo.suscripciones_Membresia rpc  
  INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
  INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
  INNER JOIN [dbo].[Frecuencia]F ON m.[IDFrecuencia]=F.[IDFrecuencia]
  WHERE rpc.CodEmp = @CodEmp AND rpc.Condicion = 'TERMINADA' AND rpc.Estado <> 'Anulada'  
    --AND rpc.Pedido_ID IN (SELECT NroRegistro FROM dbo.CtasXCobrar WHERE CodEmp = @CodEmp AND CondicionCredito = 'CANCELADO')  
    AND CAST(rpc.FechaFin AS date) <= CAST(GETDATE() AS date)  
  ORDER BY 1  
END  
END

GO

-- Consulta de Membresias: busqueda por nombre del socio.  
-- Devuelve las mismas 17 columnas que sp_ListaRegistroPeriodoClases (el formulario lee por posicion).  

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_Listasuscripciones_MembresiaXNomCli')
BEGIN
DROP PROC sp_Listasuscripciones_MembresiaXNomCli
END
GO
CREATE   PROCEDURE sp_Listasuscripciones_MembresiaXNomCli -- '01','JUAN',1  
@CodEmp varchar(2),  
@RazonSocial varchar(100),  
@Opc int  
AS  
BEGIN  
SET NOCOUNT ON  
SET XACT_ABORT ON  
  
BEGIN TRANSACTION  
  
UPDATE rpc SET Condicion = 'TERMINADA'  
FROM dbo.RegistroPeriodoClases rpc  
INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
WHERE CAST(rpc.FechaFin AS date) <= CAST(GETDATE() AS date)  
  AND rpc.CodEmp = @CodEmp  
  AND cli.RazonSocial LIKE @RazonSocial + '%'  
  AND rpc.Estado <> 'Anulada'  
  
UPDATE rpc SET Condicion = 'ACTIVA'  
FROM dbo.RegistroPeriodoClases rpc  
INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
WHERE CAST(rpc.FechaFin AS date) > CAST(GETDATE() AS date)  
  AND rpc.CodEmp = @CodEmp  
  AND cli.RazonSocial LIKE @RazonSocial + '%'  
  AND rpc.Estado <> 'Anulada'  
  
COMMIT TRANSACTION  
  
IF (@Opc = 1)  
BEGIN  
  SELECT DISTINCT rpc.NroSuscripcion_id, CodSocio = rpc.Cliente_ID, SocioCliente = cli.RazonSocial, rpc.CodigoM,  
         Periodo = m.NombreMembresia, Duracion = DATEDIFF(DAY, rpc.FechaInicio, rpc.FechaFin), F.Frecuencia,   
         rpc.FechaInicio, rpc.FechaFin, rpc.Personal_ID, rpc.SubTotal, rpc.Descuento, rpc.Total,DiasClases, rpc.Estado, rpc.Condicion, rpc.Observacion  
  FROM dbo.suscripciones_Membresia rpc  
  INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
  INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
  INNER JOIN [dbo].[Frecuencia]F ON m.[IDFrecuencia]=F.[IDFrecuencia]
  WHERE rpc.CodEmp = @CodEmp AND cli.RazonSocial LIKE @RazonSocial + '%'  
    AND rpc.Condicion = 'ACTIVA' AND rpc.Estado <> 'Anulada'  
  UNION  
  SELECT DISTINCT rpc.NroSuscripcion_id, rpc.Cliente_ID, cli.RazonSocial, rpc.CodigoM,  
         m.NombreMembresia, DATEDIFF(DAY, rpc.FechaInicio, rpc.FechaFin), F.Frecuencia,   
         rpc.FechaInicio, rpc.FechaFin, rpc.Personal_ID, rpc.SubTotal, rpc.Descuento, rpc.Total,DiasClases, rpc.Estado, rpc.Condicion, rpc.Observacion  
  FROM dbo.suscripciones_Membresia rpc  
  INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
  INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
  INNER JOIN [dbo].[Frecuencia]F ON m.[IDFrecuencia]=F.[IDFrecuencia]
  WHERE rpc.CodEmp = @CodEmp AND cli.RazonSocial LIKE @RazonSocial + '%'  
    AND rpc.Condicion = 'TERMINADA' AND rpc.Estado <> 'Anulada'  
    --AND rpc.Pedido_ID IN (SELECT NroRegistro FROM dbo.CtasXCobrar WHERE CodEmp = @CodEmp AND CondicionCredito = 'ADEUDA')  
  ORDER BY 1  
END  
ELSE  
BEGIN  
  SELECT DISTINCT rpc.NroSuscripcion_id, CodSocio = rpc.Cliente_ID, SocioCliente = cli.RazonSocial, rpc.CodigoM,  
         Periodo = m.NombreMembresia, Duracion = DATEDIFF(DAY, rpc.FechaInicio, rpc.FechaFin), F.Frecuencia,  
         rpc.FechaInicio, rpc.FechaFin, rpc.Personal_ID, rpc.SubTotal, rpc.Descuento, rpc.Total,DiasClases, rpc.Estado, rpc.Condicion, rpc.Observacion  
  FROM dbo.suscripciones_Membresia rpc  
  INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
  INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
  INNER JOIN [dbo].[Frecuencia]F ON m.[IDFrecuencia]=F.[IDFrecuencia]
  WHERE rpc.CodEmp = @CodEmp AND cli.RazonSocial LIKE @RazonSocial + '%'  
    AND rpc.Condicion = 'TERMINADA' AND rpc.Estado <> 'Anulada'  
    --AND rpc.Pedido_ID NOT IN (SELECT NroRegistro FROM dbo.CtasXCobrar WHERE CodEmp = @CodEmp)  
    AND CAST(rpc.FechaFin AS date) <= CAST(GETDATE() AS date)  
  UNION  
  SELECT DISTINCT rpc.NroSuscripcion_id,rpc.Cliente_ID, cli.RazonSocial, rpc.CodigoM,  
         m.NombreMembresia, DATEDIFF(DAY, rpc.FechaInicio, rpc.FechaFin), F.Frecuencia,  
         rpc.FechaInicio, rpc.FechaFin, rpc.Personal_ID, rpc.SubTotal, rpc.Descuento, rpc.Total,DiasClases, rpc.Estado, rpc.Condicion, rpc.Observacion  
  FROM dbo.suscripciones_Membresia rpc  
  INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
  INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
  INNER JOIN [dbo].[Frecuencia]F ON m.[IDFrecuencia]=F.[IDFrecuencia]
  WHERE rpc.CodEmp = @CodEmp AND cli.RazonSocial LIKE @RazonSocial + '%'  
    AND rpc.Condicion = 'TERMINADA' AND rpc.Estado <> 'Anulada'  
    --AND rpc.Pedido_ID IN (SELECT NroRegistro FROM dbo.CtasXCobrar WHERE CodEmp = @CodEmp AND CondicionCredito = 'CANCELADO')  
    AND CAST(rpc.FechaFin AS date) <= CAST(GETDATE() AS date)  
  ORDER BY 1  
END  
END  

GO

  
IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_Listasuscripciones_MembresiaXRangoFechas')
BEGIN
DROP PROC sp_Listasuscripciones_MembresiaXRangoFechas
END
GO
CREATE proc sp_Listasuscripciones_MembresiaXRangoFechas    
@CodEmp varchar(2),    
@FechaIni datetime,    
@FechaFin datetime,    
@Opc int    
as    
    
IF(@Opc=1)    
    
BEGIN    
select DISTINCT NroSuscripcion_id,CodSocio=(rpc.Cliente_ID),SocioCliente=(cli.RazonSocial),rpc.CodigoM,    
       Periodo=(m.NombreMembresia),Duracion=(Duracion_Dias),F.Frecuencia,FechaInicio,FechaFin,Personal_ID,SubTotal,Descuento,Total,    
       DiasClases,rpc.Estado,Condicion,Observacion    
from dbo.suscripciones_Membresia rpc    
INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID=cli.Cliente_ID    
INNER JOIN dbo.Membresia M ON rpc.CodigoM=M.CodigoM 
INNER JOIN [dbo].[Frecuencia]F ON m.[IDFrecuencia]=F.[IDFrecuencia]
WHERE CodEmp=@CodEmp    
  and FechaInicio between @FechaIni and @FechaFin    
  and Condicion='ACTIVA'    
   END    
ELSE     
BEGIN    
select DISTINCT NroSuscripcion_id,CodSocio=(rpc.Cliente_ID),SocioCliente=(cli.RazonSocial),rpc.CodigoM,    
       Periodo=(m.NombreMembresia),Duracion=(Duracion_Dias),F.Frecuencia,FechaInicio,FechaFin,Personal_ID,SubTotal,Descuento,Total,   
       DiasClases,rpc.Estado,Condicion,Observacion    
from dbo.suscripciones_Membresia rpc    
INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID=cli.Cliente_ID    
INNER JOIN dbo.Membresia M ON rpc.CodigoM=M.CodigoM    
INNER JOIN [dbo].[Frecuencia]F ON m.[IDFrecuencia]=F.[IDFrecuencia]
WHERE CodEmp=@CodEmp    
  and FechaInicio between @FechaIni and @FechaFin    
  and Condicion='TERMINADA'    
      
ORDER BY NroSuscripcion_id ASC    
    
 END    
    
  
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_ListaFrecuencia')
BEGIN
DROP PROC sp_ListaFrecuencia
END
GO
CREATE proc sp_ListaFrecuencia
as
select [IDFrecuencia],[Frecuencia],[Estado]FROM [dbo].[Frecuencia]

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_InsertMembresia')
BEGIN
DROP PROC sp_InsertMembresia
END
GO
CREATE PROCEDURE sp_InsertMembresia  
(  
 @CodigoM VARCHAR(4),  
 @NombreMembresia VARCHAR(50),  
 @Duracion_Dias INT,  
 @Duracion_Semanas INT,  
 @Duracion_Meses INT,  
 @Duracion_Anios INT,  
 @DiasLimite INT,   
 @Tarifa MONEY,  
 @Estado CHAR(10),  
 @IDFrecuencia INT  
)  
AS  
BEGIN  
 INSERT INTO Membresia  
 (  
  CodigoM,  
  NombreMembresia,  
  Duracion_Dias,  
  Duracion_Semanas,  
  Duracion_Meses,  
  Duracion_Anios,  
  DiasLimite,  
  Tarifa,  
  Estado,
  IDFrecuencia 
 )  
 VALUES  
 (  
  @CodigoM,  
  @NombreMembresia,  
  @Duracion_Dias,  
  @Duracion_Semanas,  
  @Duracion_Meses,  
  @Duracion_Anios,  
  @DiasLimite,  
  @Tarifa,  
  @Estado,  
  @IDFrecuencia  
 )  
END  

GO
  
IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_UpdateMembresia')
BEGIN
DROP PROC sp_UpdateMembresia
END
GO
CREATE PROCEDURE sp_UpdateMembresia  
(  
 @CodigoM VARCHAR(4),  
 @NombreMembresia VARCHAR(50),  
 @Duracion_Dias INT,  
 @Duracion_Semanas INT,  
 @Duracion_Meses INT,  
 @Duracion_Anios INT,  
 @DiasLimite INT,  
 @Tarifa MONEY,  
 @Estado CHAR(10),  
 @IDFrecuencia INT  
)  
AS  
BEGIN  
 UPDATE Membresia  
 SET  
  NombreMembresia = @NombreMembresia,  
  Duracion_Dias = @Duracion_Dias,  
  Duracion_Semanas = @Duracion_Semanas,  
  Duracion_Meses = @Duracion_Meses,  
  Duracion_Anios = @Duracion_Anios,  
  DiasLimite = @DiasLimite,    
  Tarifa = @Tarifa,  
  Estado = @Estado,  
  IDFrecuencia = @IDFrecuencia  
 WHERE CodigoM = @CodigoM  
END  
  

GO

ALTER TABLE DiasMembresia DROP COLUMN UsuarioMod
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_InsertDiasMembresia')
BEGIN
DROP PROC sp_InsertDiasMembresia
END
GO
CREATE proc sp_InsertDiasMembresia  
@CodigoDM varchar(4),  
@CodDia int,  
@Dia varchar(20)
  
as  
  
INSERT INTO DiasMembresia(CodigoDM,CodDia,Dia) 
VALUES(@CodigoDM,@CodDia,@Dia)  
   
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_Listasuscripciones_MembresiaXCodMembresia')
BEGIN
DROP PROC sp_Listasuscripciones_MembresiaXCodMembresia
END
GO
CREATE proc sp_Listasuscripciones_MembresiaXCodMembresia  
@CodEmp varchar(2),
@CodigoM varchar(4)  
as  
SELECT  NroSuscripcion_id,cliente_id,CodigoM,FechaInicio,FechaFin,Personal_ID,SubTotal,Descuento,
	    Total,Estado,Condicion,Observacion 
FROM dbo.suscripciones_Membresia  
WHERE CodEmp=@CodEmp  
  AND CodigoM=@CodigoM  
  
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_DeleteMembresia')
BEGIN
DROP PROC sp_DeleteMembresia
END
GO
CREATE proc sp_DeleteMembresia  
@CodigoM varchar(4)  
  
as    
DELETE FROM Membresia WHERE CodigoM=@CodigoM  

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_autogenera_codMembresia')
BEGIN
DROP PROC sp_autogenera_codMembresia
END
GO  
CREATE   proc sp_autogenera_codMembresia 
as  
SELECT RIGHT('0000' + CAST(ISNULL(MAX(CAST(CodigoM AS INT)), 0) + 1 AS VARCHAR(4)), 4) 
FROM Membresia; 


GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE ='P'AND NAME = 'sp_Eliminasuscripciones_Membresia')
BEGIN
DROP PROC sp_Eliminasuscripciones_Membresia
END
GO 
CREATE PROCEDURE sp_Eliminasuscripciones_Membresia
    @CodEmp varchar(2),  
    @NroSuscripcion_id char(8)
AS    
BEGIN
    SET NOCOUNT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- 1. Se incluye @CodEmp si la tabla detalle cuenta con esa columna
        DELETE FROM [DetSuscripcionPeriodoClases] 
        WHERE NroSuscripcion_id = @NroSuscripcion_id

        DELETE FROM [suscripciones_Membresia] 
        WHERE NroSuscripcion_id = @NroSuscripcion_id AND CodEmp = @CodEmp;

        COMMIT TRANSACTION;
        RETURN 0; -- Éxito
    END TRY
    BEGIN CATCH
        -- Si ocurre cualquier error en alguno de los DELETE, revierte todo
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        RETURN -1; -- Error
    END CATCH
END

GO

ALTER PROC [dbo].[sp_EliminaDataParcial]
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- 1. DESACTIVAR temporalmente las restricciones de Foreign Keys
        EXEC sp_MSforeachtable "ALTER TABLE ? NOCHECK CONSTRAINT ALL";

        -- 2. ELIMINAR DATOS DE TODAS LAS TABLAS
        DELETE FROM [dbo].[Alerta];
        DELETE FROM [dbo].[Auditoria_Sistema];

        DELETE FROM [dbo].[NumeracionComp1];
        DELETE FROM [dbo].[NumeracionComp2];
        DELETE FROM [dbo].[NumeracionComp3];
        DELETE FROM [dbo].[NumeracionOP];

        DELETE FROM [dbo].[Detalle_Boleta];
        DELETE FROM [dbo].[Detalle_Cotizacion];
        DELETE FROM [dbo].[Detalle_Factura];
        DELETE FROM [dbo].[Detalle_Guia];
        DELETE FROM [dbo].[Detalle_Nota_Venta];
        DELETE FROM [dbo].[Detalle_permisos];
        DELETE FROM [dbo].[Detalle_Registro_compra];
        DELETE FROM [dbo].[Detalle_Ventas];
        DELETE FROM [dbo].[DetalleNota_Credito];
		DELETE FROM [dbo].[DetRegistroAsistencias]
		DELETE FROM [dbo].[DetRegistroPeriodoClases]
		DELETE FROM [dbo].[DetSuscripcionPeriodoClases]

        DELETE FROM [dbo].[DetalleCtasXCobrar];
        DELETE FROM [dbo].[DetalleCtasXPagar];
        DELETE FROM [dbo].[DetalleFormasPago];
        DELETE FROM [dbo].[TempDetalleFormasPago];

        DELETE FROM [dbo].[Cont_DetCompVentas];
        DELETE FROM [dbo].[TempMovimientoDia_Almacen];
        DELETE FROM [dbo].[Detalle_Inventario];

        DELETE FROM [dbo].[Boleta];
        DELETE FROM [dbo].[Factura];
        DELETE FROM [dbo].[Nota_Venta];
        DELETE FROM [dbo].[Cotizacion];
        DELETE FROM [dbo].[Nota_Credito];
        DELETE FROM [dbo].[Cabecera_Ventas];
        DELETE FROM [dbo].[Guia_de_Remision];
        DELETE FROM [dbo].[Registro_de_Compra];

        DELETE FROM [dbo].[Cont_CabCompVentas];
        DELETE FROM [dbo].[CtasXCobrar];
        DELETE FROM [dbo].[CtasXPagar];
        DELETE FROM [dbo].[Inventario];

        DELETE FROM [dbo].[GastosDiversos];
        DELETE FROM [dbo].[RegistroPeriodoClases];
		DELETE FROM [dbo].[suscripciones_Membresia]
        DELETE FROM [dbo].[PersonalPagos];
        DELETE FROM [dbo].[AperturaCierreCaja];

        -- 3. RESETEAR AUTOMÁTICAMENTE LOS IDENTITY A 0 (Siguiente registro será 1)
        -- Solo aplica a las tablas listadas arriba que tengan columna IDENTITY
        DECLARE @sqlReseed NVARCHAR(MAX) = N'';

        SELECT @sqlReseed += 'DBCC CHECKIDENT (''' + QUOTENAME(SCHEMA_NAME(t.schema_id)) + '.' + QUOTENAME(t.name) + ''', RESEED, 0);' + CHAR(13)
        FROM sys.tables t
        INNER JOIN sys.identity_columns ic ON t.object_id = ic.object_id
        WHERE SCHEMA_NAME(t.schema_id) = 'dbo'
          AND t.name IN (
              'Alerta', 'Auditoria_Sistema',
              'NumeracionComp1', 'NumeracionComp2', 'NumeracionComp3', 'NumeracionOP',
              'Detalle_Boleta', 'Detalle_Cotizacion', 'Detalle_Factura', 'Detalle_Guia',
              'Detalle_Nota_Venta', 'Detalle_permisos', 'Detalle_Registro_compra',
              'Detalle_Ventas', 'DetalleNota_Credito',
              'DetalleCtasXCobrar', 'DetalleCtasXPagar', 'DetalleFormasPago', 'TempDetalleFormasPago',
              'Cont_DetCompVentas', 'TempMovimientoDia_Almacen', 'Detalle_Inventario',
              'Boleta', 'Factura', 'Nota_Venta', 'Cotizacion', 'Nota_Credito',
              'Cabecera_Ventas', 'Guia_de_Remision', 'Registro_de_Compra',
              'Cont_CabCompVentas', 'CtasXCobrar', 'CtasXPagar', 'Inventario',
              'GastosDiversos', 'RegistroPeriodoClases', 'PersonalPagos', 'AperturaCierreCaja'
          );

        EXEC sp_executesql @sqlReseed;

        -- 4. REACTIVAR todas las restricciones de Foreign Keys
        EXEC sp_MSforeachtable "ALTER TABLE ? WITH CHECK CHECK CONSTRAINT ALL";

        -- 5. EJECUTAR PROCEDIMIENTO DE CONFIGURACIÓN
        EXEC sp_InicializaConfiguracionSeries;

        COMMIT TRANSACTION;
        RETURN 0;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        THROW;
    END CATCH
END
GO

ALTER   PROCEDURE [dbo].[sp_InsertAperturaCierreCaja]
 @FechaTurno datetime,
 @FechaCaja datetime,
 @HoraIni varchar(21),
 @CodResp int,
 @MontoInicial money,
 @VentaDia money,
 @VentaContado money,
 @VentaCredito money,
 @VentaEfectivo money,
 @VentaDeposito money,
 @TotalPagos money,
 @TotalGastos money,
 @TotalCajaParc money,
 @HoraCierre varchar(20),
 @EstadoCaja varchar(2),
 @EfectivoActual money,
 @Turno varchar(2),
 @CodEmp varchar(2),
 @UsuarioMod int
AS
BEGIN
SET NOCOUNT ON
SET XACT_ABORT ON
BEGIN TRY
  BEGIN TRANSACTION

  INSERT INTO AperturaCierreCaja
  VALUES (@FechaTurno, @FechaCaja, @HoraIni, @CodResp, @MontoInicial, @VentaDia, @VentaContado, @VentaCredito,
          @VentaEfectivo, @VentaDeposito, @TotalPagos, @TotalGastos, @TotalCajaParc, @HoraCierre, @EstadoCaja,
          @EfectivoActual, @Turno, @CodEmp, @UsuarioMod)

  DECLARE @idAperturaCierre int
  SELECT @idAperturaCierre = (SELECT MAX(idAperturaCierre) FROM AperturaCierreCaja
                              WHERE CodEmp = @CodEmp AND FechaTurno = @FechaTurno)

  --EXEC sp_InsertTempMovimientoDia_Almacen @FechaTurno, @CodEmp, @idAperturaCierre, @HoraIni, @Turno

  COMMIT TRANSACTION
  SELECT 0 AS RESULT
END TRY
BEGIN CATCH
  IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION;
  THROW;
END CATCH
END

GO

ALTER  PROC [dbo].[sp_IngresoUsuarioSistema]  
@Usuario varchar(50),  
@Contraseña varchar(50)  
AS  
SELECT per.Personal_ID, ApellidoPaterno, ApellidoMaterno, Nombres, per.Area_ID,  
       Area = (DescripcionArea), Rol = (nombre_rol), CodEmp,usuario_id,
	   Usuario,RazonSocial,RazonComercial,TipoSistema,E.Ruc,CodigoProv
FROM Personal per  
INNER JOIN Area ar ON per.Area_ID = ar.Area_ID  
INNER JOIN Usuario U ON per.Personal_ID = U.Personal_ID  
INNER JOIN Rol R ON U.rol_id = R.rol_id  
INNER JOIN Empresa E ON CodEmp=Codigo_Emp
WHERE Usuario = @Usuario  
  AND [Contraseña] = @Contraseña  
  AND ISNULL(per.EstadoGene, 'Activo') = 'Activo'  
  

GO
-------------------------------------------ALERTAS ---------------------------------------------------------
GO

CREATE PROC [dbo].[sp_InsertDetRegistroAsistencias]
    @NroSuscripcion_id CHAR(8),
    @Cliente_ID CHAR(8),
    @FechaHora DATETIME,
    @FechaFin DATETIME,
    @Turno VARCHAR(2)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        DECLARE @IDSecuencia INT;

        -- Obtiene el siguiente correlativo de forma segura evitando duplicados simultáneos
        SELECT @IDSecuencia = ISNULL(MAX(IDSecuencia), 0) + 1
        FROM dbo.DetRegistroAsistencias WITH (UPDLOCK, HOLDLOCK)
        WHERE NroSuscripcion_id = @NroSuscripcion_id
          AND Cliente_ID = @Cliente_ID;

        INSERT INTO dbo.DetRegistroAsistencias (
            NroSuscripcion_id,
            Cliente_ID,
            IDSecuencia,
            HoraIngreso,
            FechaIngreso,
            Turno
        )
        VALUES (
            @NroSuscripcion_id,
            @Cliente_ID,
            @IDSecuencia,
            @FechaHora,
            @FechaFin,
            @Turno
        );

        COMMIT TRANSACTION;
        RETURN 0;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        THROW;
    END CATCH
END

GO

CREATE proc sp_ListaDetRegistroAsistenciasXRegistro--'00000001','40424341'  
 @NroSuscripcion_id char(8),  
 @Cliente_ID char(8)  
  
as  
  
SELECT Dia=(IDSecuencia),HoraIngreso=CONVERT(CHAR(10),HoraIngreso,108),  
       FechaIngreso=CONVERT(CHAR(10),FechaIngreso,101)   
FROM dbo.DetRegistroAsistencias  
WHERE NroSuscripcion_id=@NroSuscripcion_id  
  and Cliente_ID=@Cliente_ID

GO

CREATE proc sp_ListaDetRegistroAsistenciasXRegistroXFechaIng  
 @NroSuscripcion_id char(8),  
 @Cliente_ID char(8), 
 @FechaIngreso datetime  
  
as  
  
SELECT [NroSuscripcion_id],[Cliente_ID],IDSecuencia, [HoraIngreso],[FechaIngreso],[Turno]
FROM dbo.DetRegistroAsistencias  
WHERE NroSuscripcion_id=@NroSuscripcion_id  
  and Cliente_ID=@Cliente_ID  
  and FechaIngreso=@FechaIngreso  

GO

  
ALTER PROC [dbo].[sp_InsertaAlerta]
    @idAlerta INT,
    @Nombre VARCHAR(20)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        -- Elimina directamente cualquier alerta previa sin hacer COUNT(*) innecesario
        DELETE FROM dbo.Alerta;

        -- Especifica siempre las columnas explícitas al insertar
        INSERT INTO dbo.Alerta (idAlerta, Nombre)
        VALUES (@idAlerta, @Nombre);

        COMMIT TRANSACTION;
        RETURN 0;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
            ROLLBACK TRANSACTION;

        THROW;
    END CATCH
END

GO

ALTER  proc sp_lista_ClienteXcod --'000001'      
@cliente_id char(8)      
as      
select Cliente_ID,RazonSocial,Direccion,Telefono,Estado,Telefono2,Correo,Sexo,RucDNI,TipoId,    
       Foto,Contacto,TelefonoCont, HuellaDactilar    
from cliente   
where cliente_id=@cliente_id      
      

GO

  
CREATE proc sp_ListaNroSuscripciones_MembresiaXAlumno--'01','70012717'    
@CodEmp varchar(2),            
@Cliente_ID char(8)            
as            
            
select NroSuscripcion_id,[Estado],[Condicion],[Observacion] from dbo.suscripciones_Membresia          
WHERE CodEmp=@CodEmp            
  and Cliente_ID=@Cliente_ID         
  and Estado<>'Anulada'           
  and Condicion='ACTIVA'            
              
GO

ALTER PROC [dbo].[sp_Listasuscripciones_MembresiaXAlumnoXRegistro] -- '01','40424340','00000004'
    @CodEmp VARCHAR(2),
    @Cliente_ID CHAR(8),
    @NroSuscripcion_id CHAR(8)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT 
        rpc.NroSuscripcion_id,
        CodSocio    = rpc.Cliente_ID,
        SocioCliente = cli.RazonSocial,
        rpc.CodigoM,
        Membresia   = m.NombreMembresia,
        Duracion    = M.Duracion_Dias, -- Verifica si Duracion_Dias pertenece a rpc o a m
        f.Frecuencia,
        rpc.FechaInicio,
        rpc.FechaFin,
        rpc.Personal_ID,
        rpc.SubTotal,
        rpc.Descuento,
        rpc.Total,
        rpc.Estado,
        rpc.Condicion,
        rpc.Observacion
    FROM dbo.suscripciones_Membresia AS rpc
    INNER JOIN dbo.Cliente AS cli 
        ON rpc.Cliente_ID = cli.Cliente_ID
        -- Si Cliente es multempresa, descomenta la siguiente línea:
        -- AND rpc.CodEmp = cli.CodEmp
    INNER JOIN dbo.Membresia AS m 
        ON rpc.CodigoM = m.CodigoM
        -- Si Membresia es multempresa, descomenta la siguiente línea:
        -- AND rpc.CodEmp = m.CodEmp
    INNER JOIN dbo.Frecuencia AS f 
        ON m.IDFrecuencia = f.IDFrecuencia
    WHERE rpc.CodEmp = @CodEmp
      AND rpc.Cliente_ID = @Cliente_ID
      AND rpc.NroSuscripcion_id = @NroSuscripcion_id;
      -- AND rpc.Condicion = 'ACTIVA'
END

GO

  
ALTER  proc sp_ListaAperturaCierreCajaAbierto--'01','01'          
@CodEmp varchar(2)         
            
as            
select [idAperturaCierre],[FechaTurno],[FechaCaja],[HoraIni],[CodResp],[MontoInicial],[VentaDia],
	   [VentaContado],[VentaCredito],[VentaEfectivo],[VentaDeposito],[TotalPagos],[TotalGastos],
	   [TotalCajaParc],[HoraCierre],[EstadoCaja],[EfectivoActual],[Turno],[CodEmp]
from dbo.AperturaCierreCaja where CodEmp=@CodEmp and EstadoCaja='A'      
  

GO

CREATE PROC [dbo].[sp_ListaCantMaximaRegistroAsistenciasXAlumno] -- '01','40424328','00000001'
    @CodEmp VARCHAR(2),
    @Cliente_ID CHAR(8),
    @NroSuscripcion_id CHAR(8)
AS
BEGIN
    SET NOCOUNT ON;

    SELECT 
        Dias = m.Duracion_Dias, -- Verifica si Duracion_Dias está en 'm' o en 'rpc'
        CANT = COUNT(drpc.IDSecuencia)
    FROM dbo.suscripciones_Membresia AS rpc
    INNER JOIN dbo.Membresia AS m 
        ON rpc.CodigoM = m.CodigoM
        -- Si Membresia tiene CodEmp, descomenta la siguiente línea:
        -- AND rpc.CodEmp = m.CodEmp
    LEFT JOIN dbo.DetRegistroAsistencias AS drpc 
        ON rpc.NroSuscripcion_id = drpc.NroSuscripcion_id
        AND rpc.Cliente_ID = drpc.Cliente_ID
        -- Si DetRegistroAsistencias tiene CodEmp, descomenta la siguiente línea:
        -- AND rpc.CodEmp = drpc.CodEmp
    WHERE rpc.CodEmp = @CodEmp
      AND rpc.Cliente_ID = @Cliente_ID
      AND rpc.NroSuscripcion_id = @NroSuscripcion_id
    GROUP BY m.Duracion_Dias;
END

GO
 
  
ALTER proc sp_ListaCtasXCobrarXClienteXPedido--'01','40424328','00000007'  
@CodEmp varchar(2),  
@Cliente_ID varchar(20),  
@NroPedido char(10)  
as  
  
select  Cobro_ID,cxc.Cliente_ID,RazonSocial,Direccion,NroRegistro,FechaEmi,TipoDocumento,NroDocumento,  
        Monto,NroDias,Interes,Personal_ID,SaldoCredito,CondicionCredito,FechaCanc,cxc.CodEmp,UsuarioMod,  
        FechaVenc  
from dbo.CtasXCobrar cxc  
INNER JOIN dbo.Cliente cli ON cxc.Cliente_ID=cli.Cliente_ID  
where cxc.CodEmp=@CodEmp   
  and cli.Cliente_ID=@Cliente_ID   
  and NroRegistro=@NroPedido  
    
GO
* 
CREATE proc sp_ListaDetRegistroAsistenciasXFechaXTurno--'00000001','40424341'        
 @CodEmp varchar(2),    
 @FechaIngreso datetime,    
 @Turno varchar(2)      
      
as      
      
SELECT Dia=(IDSecuencia),drpc.Cliente_ID,RazonSocial,CAST(HoraIngreso AS time),      
       FechaIngreso=CAST(FechaIngreso AS DATE),CondicionCredito   
  
FROM dbo.DetRegistroAsistencias drpc  
INNER JOIN dbo.suscripciones_Membresia rpc ON drpc.NroSuscripcion_id=rpc.NroSuscripcion_id  
INNER JOIN dbo.Cliente cli ON drpc.Cliente_ID=cli.Cliente_ID   
INNER JOIN dbo.CtasXCobrar cxc ON drpc.Cliente_ID=cxc.Cliente_ID  
        
    
WHERE cxc.CodEmp=@CodEmp   
  and FechaIngreso=@FechaIngreso      
  and Turno=@Turno    
     
UNION  
  
SELECT Dia=(IDSecuencia),drpc.Cliente_ID,RazonSocial,CAST(HoraIngreso AS time),      
       FechaIngreso=CAST(FechaIngreso AS DATE),CondicionCredito='CANCELADO'   
  
FROM dbo.DetRegistroAsistencias drpc  
INNER JOIN dbo.suscripciones_Membresia rpc ON drpc.NroSuscripcion_id=rpc.NroSuscripcion_id 
INNER JOIN dbo.Cliente cli ON drpc.Cliente_ID=cli.Cliente_ID  
    
WHERE rpc.CodEmp=@CodEmp   
  and FechaIngreso=@FechaIngreso  
  and Turno=@Turno     
  and drpc.Cliente_ID NOT IN(SELECT Cliente_ID FROM dbo.CtasXCobrar WHERE CodEmp=@CodEmp)   
   

GO
* 
CREATE proc sp_lista_Codigoclientes_Activos    
as    
select Cliente_ID from cliente where Estado='Activo'    

GO

/* Prueba:
EXEC dbo.sp_Consulta_VentasXDias '01', '20261101', '20261130', ''
*/


/* Prueba:
EXEC dbo.sp_Consulta_VentasXDias '01', '20261101', '20261130', ''
*/


public class _Procedimientos2 {
    
}
