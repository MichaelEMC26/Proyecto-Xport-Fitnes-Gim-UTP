/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dto;

/**
 *
CREATE TABLE [dbo].[Movimiento_Almacen](
	[MovimientoAlm_ID] [int] IDENTITY(1,1) NOT NULL,
	[idproducto] [char](6) NOT NULL,
	[FechaMovimiento] [datetime] NULL,
	[TipoMovimiento] [int] NULL,
	[Proveedor_Cliente] [varchar](100) NULL,
	[CantIngreso] [real] NULL,
	[PrecioCompra] [money] NULL,
	[CantSalida] [real] NULL,
	[PrecioVenta] [money] NULL,
	[Saldo] [real] NULL,
	[NroRegistro] [char](10) NULL,
	[CodEmp] [varchar](2) NULL,
	[CodSede] [varchar](2) NULL,
	[Hora] [varchar](20) NULL,
	[OrdenMov] [varchar](8) NULL,
	[CodResp] [int] NULL,
 CONSTRAINT [PK_Movimiento_Almacen] PRIMARY KEY CLUSTERED 
(
	[MovimientoAlm_ID] ASC,
	[idproducto] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO

* CREATE TABLE [dbo].[Inventario](
	[Inventario_ID] [char](6) NOT NULL,
	[FechaInicio] [datetime] NULL,
	[FechaCierre] [datetime] NULL,
	[Personal_ID] [int] NOT NULL,
	[Responsable] [varchar](50) NULL,
	[Descripcion] [varchar](100) NULL,
	[totalItemsBase] [real] NULL,
	[totalItemsAct] [real] NULL,
	[BaseImpon] [money] NULL,
	[IGV] [money] NULL,
	[TotalCosto] [money] NULL,
	[TotalVenta] [money] NULL,
	[TotalMargen] [money] NULL,
	[PromPorcen] [money] NULL,
	[CodEmpresa] [varchar](2) NULL,
	[FlagInvAlm] [varchar](2) NULL,
 CONSTRAINT [pk_Inventario] PRIMARY KEY CLUSTERED 
(
	[Inventario_ID] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
GO
* 
IF NOT EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'U'AND NAME = 'MovimientoDiaStock')
BEGIN
CREATE TABLE [dbo].[MovimientoDiaStock](
	[IDCodigo] [int] IDENTITY(1,1) NOT NULL,
	[idproducto] [char](6)NOT NULL,
	[FechaMovimiento] [datetime] NULL,
	[SaldoInicial] [real] NULL,
	[SaldoFinal] [real] NULL,
	[CodEmp] [char](2) NOT NULL,
	[CodLocal] [char](2) NOT NULL,
	[FlagIni][int],
 CONSTRAINT [PK_MovimientoDiaStock] PRIMARY KEY CLUSTERED 
(
	[IDCodigo] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON) ON [PRIMARY]
) ON [PRIMARY]
 END

GO
* 
IF OBJECT_ID('dbo.sp_GeneraMovimientoDiaStock', 'P') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_GeneraMovimientoDiaStock;
END
GO
CREATE proc sp_GeneraMovimientoDiaStock  
@CodEmp varchar(2),  
--@CodLocal varchar(2),  
@FechaMovimiento datetime  
as  
BEGIN 
SET NOCOUNT ON
SET XACT_ABORT ON
 BEGIN TRY
  BEGIN TRANSACTION

	DECLARE @CodLocal VARCHAR(2)  
	SET @CodLocal='01'

IF NOT EXISTS (SELECT 1    
   FROM MovimientoDiaStock  
   WHERE CodEmp=@CodEmp  
     AND CodLocal=@CodLocal      
     AND FechaMovimiento>@FechaMovimiento)    
BEGIN  
IF EXISTS (SELECT 1  
   FROM MovimientoDiaStock  
   WHERE CodEmp=@CodEmp  
     AND CodLocal=@CodLocal   
     AND FechaMovimiento<@FechaMovimiento)
BEGIN  
  
DECLARE @UltimFechaMovimiento DATETIME  
SET @UltimFechaMovimiento=(SELECT MAX(FechaMovimiento)FROM MovimientoDiaStock WHERE CodEmp=@CodEmp AND CodLocal=@CodLocal AND FechaMovimiento<@FechaMovimiento)  
  
IF EXISTS (SELECT 1  
   FROM MovimientoDiaStock  
   WHERE FechaMovimiento=@UltimFechaMovimiento  
     AND CodEmp=@CodEmp  
     AND CodLocal=@CodLocal  
     AND FlagIni=0) 
BEGIN  
DELETE FROM MovimientoDiaStock   
WHERE [idproducto]NOT IN (SELECT [idproducto] FROM [dbo].[Movimiento_Almacen] WHERE FechaMovimiento=@UltimFechaMovimiento    
                             AND CodEmp=@CodEmp AND CodLocal=@CodLocal)    
                             AND CodEmp=@CodEmp AND CodLocal=@CodLocal   
  
UPDATE MovimientoDiaStock  
   SET [SaldoFinal]=[StockFinal],FlagIni=1  
  FROM MovimientoDiaStock MDS  
 INNER JOIN [dbo].[productos]P ON MDS.[idproducto]=P.[Producto_ID]AND MDS.CodEmp=P.CodEmp  
 WHERE FechaMovimiento=@UltimFechaMovimiento  
   AND MDS.CodEmp=@CodEmp  
   AND MDS.CodLocal=@CodLocal  
   END  
  END  
  
	IF NOT EXISTS (SELECT 1   
	   FROM MovimientoDiaStock  
	   WHERE CodEmp=@CodEmp  
		 AND CodLocal=@CodLocal  
		 AND FechaMovimiento=@FechaMovimiento)  
	BEGIN  
	INSERT INTO MovimientoDiaStock  
	SELECT [Producto_ID],@FechaMovimiento,[StockFinal],0,@CodEmp,@CodLocal,0  
	FROM [dbo].[productos]  
	WHERE CodEmp=@CodEmp AND [EstadoProducto]='Activo'  
	ORDER BY [Producto_ID]ASC  
	  END  
	 END 
    COMMIT TRANSACTION
	  RETURN 0
	END TRY
	BEGIN CATCH
	IF @@TRANCOUNT >0
	 BEGIN
	 ROLLBACK TRANSACTION
	  END
	 RETURN -1
	 END CATCH
    END  

GO

IF OBJECT_ID('dbo.sp_ListaMovimientosDiarioProductos', 'P') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_ListaMovimientosDiarioProductos;
END
GO
CREATE PROC sp_ListaMovimientosDiarioProductos--'01','01','2026-09-19','2026-09-20'
    @CodEmp varchar(2),   
    --@CodLocal varchar(2),   
    @FechaIni DATETIME,  
    @FechaFin DATETIME
AS
BEGIN
DECLARE @CodLocal VARCHAR(2)  
SET @CodLocal='01'

    SET NOCOUNT ON; 

    DECLARE @SaldosIniciales TABLE (
        idproducto INT PRIMARY KEY,
        SaldoInicial DECIMAL(18,4)
    );

    DECLARE @SaldosFinales TABLE (
        idproducto INT PRIMARY KEY,
        SaldoFinal DECIMAL(18,4)
    );

    INSERT INTO @SaldosIniciales (idproducto, SaldoInicial)
    SELECT [idproducto], [SaldoInicial]
    FROM [dbo].[MovimientoDiaStock]
    WHERE CodEmp = @CodEmp
      AND CodLocal = @CodLocal
      AND [FechaMovimiento] = @FechaIni;

    INSERT INTO @SaldosFinales (idproducto, SaldoFinal)
    SELECT [idproducto], [SaldoFinal]
    FROM [dbo].[MovimientoDiaStock]
    WHERE CodEmp = @CodEmp
      AND CodLocal = @CodLocal
      AND [FechaMovimiento] = @FechaFin;

    SELECT 
        ma.[idproducto],
        p.Descripcion AS Productos,
        ISNULL(T1.SaldoInicial, 0) AS SaldoInicial,
        SUM(ma.CantIngreso) AS CantIngreso,
        SUM(ma.CantSalida) AS CantSalida,
        ISNULL(T2.SaldoFinal, 0) AS SaldoFinal,
        p.UnidadMedida,
        cat.Nombre AS Categoria
    FROM [dbo].[Movimiento_Almacen] ma
    INNER JOIN [dbo].[Productos] p 
        ON ma.[idproducto] = p.[Producto_ID] 
       AND ma.CodEmp = p.CodEmp
    INNER JOIN [dbo].[Categoria] cat 
        ON p.[Categoria_ID] = cat.[Categoria_ID]
    LEFT JOIN @SaldosIniciales T1 
        ON ma.[idproducto] = T1.[idproducto]
    LEFT JOIN @SaldosFinales T2 
        ON ma.[idproducto] = T2.[idproducto]
    WHERE ma.CodEmp = @CodEmp
      AND ma.CodSede = @CodLocal
      AND ma.[FechaMovimiento] BETWEEN @FechaIni AND @FechaFin
    GROUP BY 
        ma.[idproducto],
        p.Descripcion,
        p.UnidadMedida,
        cat.Nombre,
        T1.SaldoInicial,
        T2.SaldoFinal
    ORDER BY 
        p.Descripcion ASC;
END

GO

IF OBJECT_ID('dbo.sp_ReporteExistenciasProductos', 'P') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_ReporteExistenciasProductos;
END
GO
CREATE PROCEDURE dbo.sp_ReporteExistenciasProductos
    @CodEmp varchar(2), @Filtro varchar(100) = ''
AS
BEGIN
    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));
    SELECT 
           CodigoProd      = RTRIM(p.Producto_ID),
           NombreProducto   = RTRIM(ISNULL(p.NombreProducto, '')),
		   Categoria_ID     = RTRIM(ISNULL(p.Categoria_ID, '')),
           Categoria     = RTRIM(ISNULL(cat.Nombre, '')),
		   MarcaProducto_ID     = RTRIM(ISNULL(p.MarcaProducto_ID, '')),
           MarcaProducto     = RTRIM(ISNULL(ma.Nombre, '')),
           Descripcion   = RTRIM(ISNULL(p.Descripcion, '')),
           UnidadMedida   = RTRIM(ISNULL(p.UnidadMedida, '')),
           StockFinal    = CAST(ISNULL(p.StockFinal, 0) AS decimal(12,2)),
           PrecioCosto         = CAST(ISNULL(p.PrecioCosto, 0) AS decimal(12,2)),
           ImporteValorizado       = CAST(ISNULL((p.PrecioCosto*p.StockFinal), 0) AS decimal(12,2)),
		   TipoBien       = RTRIM(ISNULL(tb.Descripcion, ''))
      FROM dbo.Productos p
      INNER JOIN [dbo].[Categoria]cat ON p.Categoria_ID=cat.Categoria_ID
	  INNER JOIN [dbo].[MarcaProducto]ma ON p.MarcaProducto_ID=ma.MarcaProducto_ID
	  INNER JOIN [dbo].[Tipo_Bien]tb ON p.[TipoBien_ID]=tb.[TipoBien_ID]
     WHERE p.CodEmp = @CodEmp
       AND (@Filtro = ''
            OR p.NombreProducto LIKE '%' + @Filtro + '%'
            OR p.Producto_ID LIKE '%' + @Filtro + '%'
            OR p.CodigoBarra LIKE '%' + @Filtro + '%'
            OR cat.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%')
     ORDER BY p.Descripcion ASC
END

GO

IF OBJECT_ID('dbo.sp_ListaMontosExistenciasProductos', 'P') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_ListaMontosExistenciasProductos;
END
GO
CREATE PROCEDURE dbo.sp_ListaMontosExistenciasProductos
    @CodEmp varchar(2), @Filtro varchar(100) = ''
AS
BEGIN
DECLARE @CantItems REAL
DECLARE @CantItemsStock REAL
DECLARE @CantItemsStockMin REAL
DECLARE @CantItemsSinStock REAL

    SET NOCOUNT ON;
    SET @Filtro = LTRIM(RTRIM(ISNULL(@Filtro, '')));

	  SET @CantItems=(SELECT COUNT(Producto_ID)
	                  FROM dbo.Productos p
      INNER JOIN [dbo].[Categoria]cat ON p.Categoria_ID=cat.Categoria_ID
	  INNER JOIN [dbo].[MarcaProducto]ma ON p.MarcaProducto_ID=ma.MarcaProducto_ID
	  INNER JOIN [dbo].[Tipo_Bien]tb ON p.[TipoBien_ID]=tb.[TipoBien_ID]
     WHERE p.CodEmp = @CodEmp
       AND (@Filtro = ''
            OR p.NombreProducto LIKE '%' + @Filtro + '%'
            OR p.Producto_ID LIKE '%' + @Filtro + '%'
            OR p.CodigoBarra LIKE '%' + @Filtro + '%'
            OR cat.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%'))

	  SET @CantItemsStock=(SELECT COUNT(Producto_ID)
	                  FROM dbo.Productos p
      INNER JOIN [dbo].[Categoria]cat ON p.Categoria_ID=cat.Categoria_ID
	  INNER JOIN [dbo].[MarcaProducto]ma ON p.MarcaProducto_ID=ma.MarcaProducto_ID
	  INNER JOIN [dbo].[Tipo_Bien]tb ON p.[TipoBien_ID]=tb.[TipoBien_ID]
     WHERE p.CodEmp = @CodEmp AND StockFinal>0
       AND (@Filtro = ''
            OR p.NombreProducto LIKE '%' + @Filtro + '%'
            OR p.Producto_ID LIKE '%' + @Filtro + '%'
            OR p.CodigoBarra LIKE '%' + @Filtro + '%'
            OR cat.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%'))
		
	  SET @CantItemsStockMin=(SELECT COUNT(Producto_ID)
	                  FROM dbo.Productos p
      INNER JOIN [dbo].[Categoria]cat ON p.Categoria_ID=cat.Categoria_ID
	  INNER JOIN [dbo].[MarcaProducto]ma ON p.MarcaProducto_ID=ma.MarcaProducto_ID
	  INNER JOIN [dbo].[Tipo_Bien]tb ON p.[TipoBien_ID]=tb.[TipoBien_ID]
     WHERE p.CodEmp = @CodEmp AND StockFinal<=StockMinimo
       AND (@Filtro = ''
            OR p.NombreProducto LIKE '%' + @Filtro + '%'
            OR p.Producto_ID LIKE '%' + @Filtro + '%'
            OR p.CodigoBarra LIKE '%' + @Filtro + '%'
            OR cat.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%'))
	  
	  SET @CantItemsSinStock=(SELECT COUNT(Producto_ID)
	                  FROM dbo.Productos p
      INNER JOIN [dbo].[Categoria]cat ON p.Categoria_ID=cat.Categoria_ID
	  INNER JOIN [dbo].[MarcaProducto]ma ON p.MarcaProducto_ID=ma.MarcaProducto_ID
	  INNER JOIN [dbo].[Tipo_Bien]tb ON p.[TipoBien_ID]=tb.[TipoBien_ID]
     WHERE p.CodEmp = @CodEmp AND StockFinal<=0
       AND (@Filtro = ''
            OR p.NombreProducto LIKE '%' + @Filtro + '%'
            OR p.Producto_ID LIKE '%' + @Filtro + '%'
            OR p.CodigoBarra LIKE '%' + @Filtro + '%'
            OR cat.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%'))

      SELECT TotalValorizado  = CAST(ISNULL(SUM(p.PrecioCosto*p.StockFinal), 0) AS decimal(12,2)),
	         @CantItems,@CantItemsStock,@CantItemsStockMin,@CantItemsSinStock
      FROM dbo.Productos p
      INNER JOIN [dbo].[Categoria]cat ON p.Categoria_ID=cat.Categoria_ID
	  INNER JOIN [dbo].[MarcaProducto]ma ON p.MarcaProducto_ID=ma.MarcaProducto_ID
	  INNER JOIN [dbo].[Tipo_Bien]tb ON p.[TipoBien_ID]=tb.[TipoBien_ID]
     WHERE p.CodEmp = @CodEmp
       AND (@Filtro = ''
            OR p.NombreProducto LIKE '%' + @Filtro + '%'
            OR p.Producto_ID LIKE '%' + @Filtro + '%'
            OR p.CodigoBarra LIKE '%' + @Filtro + '%'
            OR cat.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%'
			OR ma.Nombre LIKE '%' + @Filtro + '%')
   
END

GO

GO
--dbo.sp_autogenera_nro_inventario
--dbo.sp_EliminaInventario
--dbo.sp_Lista_InventarioXRangoFechas

IF OBJECT_ID('dbo.sp_GeneraAperturaInventarioInicial', 'P') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_GeneraAperturaInventarioInicial;
END
GO
CREATE proc sp_GeneraAperturaInventarioInicial --'01','01','16/07/2023','16/07/2022',1      
@CodEmp varchar(2),      
--@CodTienda varchar(2),      
@FechaIni datetime,      
@FechaFin datetime,      
@CodResp INT,
@Responsable varchar(50)       
      
as      
      
BEGIN 

DECLARE @CodSede VARCHAR(2)
DECLARE @vIGV REAL      
DECLARE @totalItemsBase REAL      
DECLARE @totalItemsAct REAL      
DECLARE @BaseImpon MONEY      
DECLARE @IGV MONEY      
DECLARE @TotalCosto MONEY      
DECLARE @TotalVenta MONEY      
DECLARE @TotalMargen MONEY      
DECLARE @PromPorcen MONEY      
DECLARE @dInventarioID CHAR(8)      
DECLARE @dInventarioAlm_ID CHAR(8)      

SET NOCOUNT ON
SET XACT_ABORT ON
 BEGIN TRY
  BEGIN TRANSACTION

SET @CodSede = '01'
SET @vIGV =(SELECT ISNULL((([ValorIGV]/100)+1),1.18)FROM [dbo].[ParametrosEmpresa] WHERE [CodEmpresa]=@CodEmp)  

UPDATE [dbo].[Productos]SET [StockFinal]=0     
--UPDATE [dbo].[Detalle_Almacenes]SET [Stock]=0    
      
SELECT @dInventarioID=(select '0'+replicate('0',6-(len(isnull(cast(right(max(Inventario_ID),6)as int),0)+1)+1))            
+ cast(isnull(cast(right(max(Inventario_ID),6)as int),0)+1 as char(8)) from dbo.Inventario      
  WHERE CodEmpresa=@CodEmp)       
      
--SELECT @dInventarioAlm_ID=(select '0'+replicate('0',6-(len(isnull(cast(right(max(InventarioAlm_ID),6)as int),0)+1)+1))            
--+ cast(isnull(cast(right(max(InventarioAlm_ID),6)as int),0)+1 as char(8)) from dbo.InventarioXAlmacen      
--  WHERE CodEmpresa=@CodEmp)         
      
SET @totalItemsBase=(SELECT COUNT(*)FROM [dbo].[Productos]WHERE [CodEmp]=@CodEmp AND EstadoProducto='Activo')      
      
SET @totalItemsAct=(SELECT COUNT(*)FROM [dbo].[Productos]WHERE [CodEmp]=@CodEmp AND EstadoProducto='Activo'      
                     AND [StockFinal]>0)      
      
SET @BaseImpon=(SELECT ISNULL(SUM(PrecioCosto/@vIGV),0)FROM [dbo].[Productos]P          
                 WHERE [CodEmp]=@CodEmp      
                  AND EstadoProducto='Activo'          
                  AND [StockFinal]>0)      
      
SET @IGV=(SELECT ISNULL((SUM(PrecioCosto)-SUM(PrecioCosto/@vIGV)),0)FROM [dbo].[Productos]P           
          WHERE [CodEmp]=@CodEmp      
                  AND EstadoProducto='Activo'          
                  AND [StockFinal]>0)   
      
SET @TotalCosto=(SELECT ISNULL(SUM(PrecioCosto),0)FROM [dbo].[Productos]P      
                 WHERE [CodEmp]=@CodEmp      
                  AND EstadoProducto='Activo'          
                  AND [StockFinal]>0)    
      
SET @TotalVenta=(SELECT ISNULL(SUM(PrecioVenta),0)FROM [dbo].[Productos]P      
                 WHERE [CodEmp]=@CodEmp      
                  AND EstadoProducto='Activo'          
                  AND [StockFinal]>0)    
      
SET @TotalMargen=(@TotalVenta-@TotalCosto)      
      
SET @PromPorcen=(((@TotalVenta/(CASE @TotalCosto WHEN 0 THEN 1 ELSE @TotalCosto END))-1)*100)      
      
insert into Inventario values(@dInventarioID,@FechaIni,@FechaFin,1,'Administrador','Inventario Inicial',              
                              @totalItemsBase,@totalItemsAct,@BaseImpon,@IGV,@TotalCosto,@TotalVenta,@TotalMargen,@PromPorcen,@CodEmp,'G')        
      
--insert into InventarioXAlmacen values(@dInventarioAlm_ID,@FechaIni,@FechaFin,1,'Administrador','Inventario Inicial',              
--                              @totalItemsBase,@totalItemsAct,@BaseImpon,@IGV,@TotalCosto,@TotalVenta,@TotalMargen,@PromPorcen,      
--                              @CodEmp,1,'G')        
      
insert into Detalle_Inventario      
SELECT @dInventarioID,Producto_ID,StockFinal,PrecioCosto,importe=(PrecioCosto*StockFinal)      
FROM [dbo].[Productos]      
WHERE EstadoProducto='Activo'          
ORDER BY Producto_ID ASC      
      
--insert into Detalle_InventarioXAlmacen      
--SELECT @dInventarioAlm_ID,DA.Producto_ID,Stock,PrecioCosto,importe=(PrecioCosto*Stock)      
--FROM [dbo].[Productos2]P      
--      INNER JOIN [dbo].[Detalle_Almacenes]DA ON P.Producto_ID=DA.Producto_ID       
--                   WHERE [CodEmp]=@CodEmp      
--         AND EstadoProducto='Activo'      
--      AND [Almacen_id]=@CodAlmacen      
--               --AND [Stock]>0      
--    ORDER BY DA.Producto_ID ASC      
           
      
INSERT into dbo.Movimiento_Almacen             
     (idproducto,FechaMovimiento,TipoMovimiento,                  
     Proveedor_Cliente,CantIngreso,PrecioCompra,CantSalida,PrecioVenta,                  
     Saldo,NroRegistro,CodEmp,CodSede,Hora,OrdenMov,CodResp)                   
                  
 SELECT p.PRODUCTO_ID,@FechaIni,'12',@Responsable,StockFinal,PrecioCosto,0,                  
  0,0,@dInventarioID,P.CodEmp,@CodSede,(select convert(char(12),getdate(),114)),'',@CodResp             
 FROM dbo.Productos as p                               
    INNER JOIN categoria as c ON p.CATEGORIA_ID=c.CATEGORIA_ID                                   
    INNER JOIN dbo.MarcaProducto as m ON p.MarcaProducto_ID=m.MarcaProducto_ID                   
 where p.EstadoProducto='Activo'                  
   and P.CodEmp=@CodEmp                                 
    
 DECLARE @FechaMovimiento DATETIME  
 SET @FechaMovimiento=@FechaIni  
  
 EXEC sp_GeneraMovimientoDiaStock @CodEmp,@FechaMovimiento        
    
    COMMIT TRANSACTION
	  RETURN 0
	END TRY
	BEGIN CATCH
	IF @@TRANCOUNT >0
	 BEGIN
	 ROLLBACK TRANSACTION
	  END
	 RETURN -1
	 END CATCH
    END      

GO

IF OBJECT_ID('dbo.sp_EliminaInventario', 'P') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_EliminaInventario;
END
GO
CREATE  proc sp_EliminaInventario    
@CodEmpresa varchar(2),    
@Inventario_ID char(8)    
as    
SET NOCOUNT ON
SET XACT_ABORT ON
 BEGIN TRY
  BEGIN TRANSACTION
  
 DELETE FROM dbo.Inventario WHERE Inventario_ID=@Inventario_ID and CodEmpresa=@CodEmpresa    
 DELETE FROM dbo.Detalle_Inventario WHERE Inventario_ID=@Inventario_ID    
 DELETE FROM dbo.Movimiento_Almacen     
 WHERE TipoMovimiento=10    
   AND NroRegistro=@Inventario_ID    
   AND CodEmp=@CodEmpresa    
    OR TipoMovimiento=12    
   AND NroRegistro=@Inventario_ID    
   AND CodEmp=@CodEmpresa    
  
 COMMIT TRANSACTION
	  RETURN 0
	END TRY
	BEGIN CATCH
	IF @@TRANCOUNT >0
	 BEGIN
	 ROLLBACK TRANSACTION
	  END
	 RETURN -1
	 END CATCH     

GO

IF OBJECT_ID('dbo.sp_Lista_InventarioXRangoFechas', 'P') IS NOT NULL
BEGIN
    DROP PROCEDURE dbo.sp_Lista_InventarioXRangoFechas;
END
GO
CREATE   proc sp_Lista_InventarioXRangoFechas--'01','01/01/2015','01/01/2015'  
@CodEmpresa varchar(2),  
@FechaIni datetime,  
@FechaFin datetime  
as  

DECLARE @vIGV REAL

SET @vIGV =(SELECT ISNULL((([ValorIGV]/100)+1),1.18)FROM [dbo].[ParametrosEmpresa] WHERE [CodEmpresa]=@CodEmpresa)  

SELECT Inventario_ID,FechaInicio,FechaCierre,Personal_ID,Responsable,Descripcion,totalItemsBase,totalItemsAct,  
       BaseImpon=ISNULL(BaseImpon,(TotalCosto/@vIGV)),IGV=ISNULL(IGV,(TotalCosto-(TotalCosto/@vIGV))),TotalCosto=ISNULL(TotalCosto,0),TotalVenta=ISNULL(TotalVenta,0),  
       TotalMargen=ISNULL(TotalMargen,0),PromPorcen=ISNULL(PromPorcen,0),CodEmpresa   
from dbo.Inventario   
  where CodEmpresa=@CodEmpresa  
    and FechaInicio between @FechaIni and @FechaFin  
  
  
  

GO

--SP_DEPENDS Inventario
--GO


 */
public class _Procedimiento6 {
    
}
