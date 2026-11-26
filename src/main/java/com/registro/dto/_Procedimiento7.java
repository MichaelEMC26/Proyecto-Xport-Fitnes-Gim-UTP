/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dto;

/**
 * SCRIPT SQL: Tipo de bien y Stock mínimo en el mantenimiento de productos (RegistrarProducto.xhtml).
 * CatalogoDAO.asignarComunes envía @TipoBien_ID y @StockMinimo a los tres procedimientos, así que
 * deben aceptar esos parámetros. Ejecutar en SQL Server antes de probar Grabar / Actualizar.

-- 1) Columna StockMinimo (solo si todavía no existe)
IF COL_LENGTH('dbo.Productos', 'StockMinimo') IS NULL
BEGIN
    ALTER TABLE dbo.Productos ADD StockMinimo real NULL CONSTRAINT DF_Productos_StockMinimo DEFAULT (0)
END
GO

-- 2) Insertar
IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P' AND NAME = 'sp_Insert_Productos')
BEGIN
DROP PROC sp_Insert_Productos
END
GO
CREATE proc sp_Insert_Productos
 @Producto_ID char(6),
 @NombreProducto VARCHAR(50),
 @EstadoProducto VARCHAR(10),
 @Categoria_ID int,
 @MarcaProducto_ID int,
 @Descripcion VARCHAR(100),
 @UnidadMedida VARCHAR(20),
 @StockFinal real,
 @PrecioCosto MONEY,
 @PrecioVenta MONEY,
 @PrecioLimite MONEY,
 @MargenUtil MONEY,
 @Peso real,
 @Imagen VARCHAR(150),
 @CodigoBarra VARCHAR(50),
 @CodEmp varchar(2),
 @TipoProd varchar(2),
 @TipoBien_ID varchar(3),
 @StockMinimo real = 0
as
BEGIN
SET NOCOUNT ON
SET XACT_ABORT ON
 BEGIN TRY
   BEGIN TRANSACTION

   INSERT INTO productos (Producto_ID, NombreProducto, EstadoProducto, Categoria_ID,
                          MarcaProducto_ID, Descripcion, UnidadMedida, StockFinal, PrecioCosto, PrecioVenta, PrecioLimite,
                          MargenUtil, Peso, Imagen, CodigoBarra, CodEmp, TipoProd, TipoBien_ID, StockMinimo)
   VALUES(@Producto_ID, @NombreProducto, @EstadoProducto, @Categoria_ID,
          @MarcaProducto_ID, @Descripcion, @UnidadMedida, @StockFinal, @PrecioCosto, @PrecioVenta, @PrecioLimite,
          @MargenUtil, @Peso, @Imagen, @CodigoBarra, @CodEmp, @TipoProd, @TipoBien_ID, ISNULL(@StockMinimo, 0))

   COMMIT TRANSACTION
   RETURN 0
 END TRY
 BEGIN CATCH
   IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION
   RETURN -1
 END CATCH
END
GO

-- 3) Actualizar con stock (producto sin ventas)
IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P' AND NAME = 'sp_Update_Productos')
BEGIN
DROP PROC sp_Update_Productos
END
GO
CREATE proc sp_Update_Productos
 @Producto_ID char(6),
 @NombreProducto VARCHAR(50),
 @EstadoProducto VARCHAR(10),
 @Categoria_ID int,
 @MarcaProducto_ID char(5),
 @Descripcion VARCHAR(100),
 @UnidadMedida VARCHAR(20),
 @StockFinal real,
 @PrecioCosto MONEY,
 @PrecioVenta MONEY,
 @PrecioLimite MONEY,
 @MargenUtil MONEY,
 @Peso real,
 @Imagen VARCHAR(150),
 @CodigoBarra VARCHAR(50),
 @CodEmp varchar(2),
 @TipoProd varchar(2),
 @TipoBien_ID varchar(3),
 @StockMinimo real = 0
as
BEGIN
SET NOCOUNT ON
SET XACT_ABORT ON
 BEGIN TRY
   BEGIN TRANSACTION
   UPDATE productos SET NombreProducto = @NombreProducto, EstadoProducto = @EstadoProducto,
                        Categoria_ID = @Categoria_ID, MarcaProducto_ID = @MarcaProducto_ID,
                        Descripcion = @Descripcion, UnidadMedida = @UnidadMedida, StockFinal = @StockFinal,
                        PrecioCosto = @PrecioCosto, PrecioVenta = @PrecioVenta, PrecioLimite = @PrecioLimite,
                        MargenUtil = @MargenUtil, Peso = @Peso, Imagen = @Imagen, CodigoBarra = @CodigoBarra,
                        TipoProd = @TipoProd, TipoBien_ID = @TipoBien_ID, StockMinimo = ISNULL(@StockMinimo, 0)
    WHERE Producto_ID = @Producto_ID
      AND CodEmp = @CodEmp

   COMMIT TRANSACTION
   RETURN 0
 END TRY
 BEGIN CATCH
   IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION
   RETURN -1
 END CATCH
END
GO

-- 4) Actualizar sin stock (producto con ventas: el stock se mueve con compras / ventas)
IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P' AND NAME = 'sp_Update_Productos2')
BEGIN
DROP PROC sp_Update_Productos2
END
GO
CREATE proc sp_Update_Productos2
 @Producto_ID char(6),
 @NombreProducto VARCHAR(50),
 @EstadoProducto VARCHAR(10),
 @Categoria_ID int,
 @MarcaProducto_ID char(5),
 @Descripcion VARCHAR(100),
 @UnidadMedida VARCHAR(20),
 @PrecioCosto MONEY,
 @PrecioVenta MONEY,
 @PrecioLimite MONEY,
 @MargenUtil MONEY,
 @Peso real,
 @Imagen VARCHAR(150),
 @CodigoBarra VARCHAR(50),
 @CodEmp varchar(2),
 @TipoProd varchar(2),
 @TipoBien_ID varchar(3),
 @StockMinimo real = 0
as
BEGIN
SET NOCOUNT ON
SET XACT_ABORT ON
 BEGIN TRY
   BEGIN TRANSACTION
   UPDATE productos SET NombreProducto = @NombreProducto, EstadoProducto = @EstadoProducto,
                        Categoria_ID = @Categoria_ID, MarcaProducto_ID = @MarcaProducto_ID,
                        Descripcion = @Descripcion, UnidadMedida = @UnidadMedida,
                        PrecioCosto = @PrecioCosto, PrecioVenta = @PrecioVenta, PrecioLimite = @PrecioLimite,
                        MargenUtil = @MargenUtil, Peso = @Peso, Imagen = @Imagen, CodigoBarra = @CodigoBarra,
                        TipoProd = @TipoProd, TipoBien_ID = @TipoBien_ID, StockMinimo = ISNULL(@StockMinimo, 0)
    WHERE Producto_ID = @Producto_ID
      AND CodEmp = @CodEmp

   COMMIT TRANSACTION
   RETURN 0
 END TRY
 BEGIN CATCH
   IF @@TRANCOUNT > 0 ROLLBACK TRANSACTION
   RETURN -1
 END CATCH
END
GO

 */
public class _Procedimiento7 {

}
