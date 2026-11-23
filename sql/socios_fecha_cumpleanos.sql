-- =====================================================================================
-- Socios: Fecha de cumpleaños (socios.xhtml -> ClienteDAO).
-- ClienteDAO envía @FechaCumpleaños como parámetro 15 en sp_Insert_Clientes y sp_Actualiza_Cliente,
-- y lee FechaCumpleaños en sp_lista_clientes_Activos / sp_lista_ClienteXcod (para editar sin perderla).
--
-- IMPORTANTE: sp_Actualiza_Cliente no está en los scripts del proyecto. Si su versión en la base de
-- datos hace algo más (auditoría, etc.), agregue solo el parámetro y la columna en lugar de reemplazarlo.
-- =====================================================================================

-- 1) Columna (solo si aún no existe)
IF COL_LENGTH('dbo.Cliente', 'FechaCumpleaños') IS NULL
    ALTER TABLE dbo.Cliente ADD FechaCumpleaños DATE NULL;
GO

-- 2) Registrar socio (misma lógica de _Procedimientos2.java + FechaCumpleaños)
CREATE OR ALTER PROCEDURE [dbo].[sp_Insert_Clientes]
 @Cliente_ID CHAR(8),
 @RazonSocial VARCHAR (100),
 @Direccion VARCHAR (100),
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
 @HuellaDactilar VARBINARY(MAX),
 @FechaCumpleaños DATE = NULL
AS
 BEGIN TRANSACTION

  IF EXISTS(SELECT 1 FROM cliente WHERE Cliente_ID=@Cliente_ID)
  BEGIN
  UPDATE cliente SET RazonSocial=@RazonSocial,Direccion=@Direccion,Telefono=@Telefono,
         Telefono2=@Telefono2,Correo=@Correo,RucDNI=@RucDNI,TipoId=@TipoId,
         HuellaDactilar=ISNULL(@HuellaDactilar, HuellaDactilar),
         FechaCumpleaños=@FechaCumpleaños
  WHERE Cliente_ID=@Cliente_ID
  END
  ELSE
  BEGIN
  INSERT INTO cliente(Cliente_ID,RazonSocial,Direccion,Telefono,Estado,Telefono2,Correo,Sexo,RucDNI,TipoId,
         Foto,Contacto,TelefonoCont,HuellaDactilar,FechaCumpleaños)
  VALUES(@Cliente_ID,@RazonSocial,@Direccion,@Telefono,@Estado,@Telefono2,@Correo,@Sexo,@RucDNI,@TipoId,
         @Foto,@Contacto,@TelefonoCont,@HuellaDactilar,@FechaCumpleaños)
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

-- 3) Actualizar socio (15 parámetros, mismo orden que ClienteDAO.actualizarCliente)
CREATE OR ALTER PROCEDURE [dbo].[sp_Actualiza_Cliente]
 @Cliente_ID CHAR(8),
 @RazonSocial VARCHAR (100),
 @Direccion VARCHAR (100),
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
 @HuellaDactilar VARBINARY(MAX),
 @FechaCumpleaños DATE = NULL
AS
BEGIN
  UPDATE cliente SET RazonSocial=@RazonSocial, Direccion=@Direccion, Telefono=@Telefono, Estado=@Estado,
         Telefono2=@Telefono2, Correo=@Correo, Sexo=@Sexo, RucDNI=@RucDNI, TipoId=@TipoId,
         Foto=@Foto, Contacto=@Contacto, TelefonoCont=@TelefonoCont,
         HuellaDactilar=ISNULL(@HuellaDactilar, HuellaDactilar),
         FechaCumpleaños=@FechaCumpleaños
  WHERE Cliente_ID=@Cliente_ID
END
GO

-- 4) Consultas: devolver también FechaCumpleaños
CREATE OR ALTER PROCEDURE sp_lista_clientes_Activos
AS
SELECT Cliente_ID,RazonSocial,Direccion,Telefono,Estado,Telefono2,Correo,Sexo,RucDNI,TipoId,
       Foto,Contacto,TelefonoCont,HuellaDactilar,FechaCumpleaños
FROM cliente WHERE Estado='Activo'
ORDER BY RazonSocial ASC
GO

CREATE OR ALTER PROCEDURE sp_lista_ClienteXcod --'000001'
@cliente_id char(8)
AS
SELECT Cliente_ID,RazonSocial,Direccion,Telefono,Estado,Telefono2,Correo,Sexo,RucDNI,TipoId,
       Foto,Contacto,TelefonoCont,HuellaDactilar,FechaCumpleaños
FROM cliente
WHERE cliente_id=@cliente_id
GO
