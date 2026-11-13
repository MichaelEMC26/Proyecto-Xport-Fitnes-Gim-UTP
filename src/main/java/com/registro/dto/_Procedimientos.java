/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dto;

/**
 *
 IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_IngresoUsuarioSistema')
BEGIN
DROP PROC sp_IngresoUsuarioSistema
 END
GO
CREATE proc sp_IngresoUsuarioSistema
@Usuario varchar(50),
@Contraseña varchar(50)
as

select Personal_ID,ApellidoPaterno,ApellidoMaterno,Nombres,per.Area_ID,Area=(DescripcionArea),CodEmp
from UsuarioPersonal per
INNER JOIN Area ar ON per.Area_ID=ar.Area_ID
where Usuario=@Usuario
  and Contraseña=@Contraseña

GO
UPDATE UsuarioPersonal SET Nombres='ADMINISTRADOR',[Contraseña]='12345*'WHERE Personal_ID=1
GO
UPDATE UsuarioPersonal SET Nombres='VENTAS',[Contraseña]='123',[Area_ID]='VEN'WHERE Personal_ID=2
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_ListaTurnoAbierto')
BEGIN
DROP PROC sp_ListaTurnoAbierto
 END
GO
CREATE   proc sp_ListaTurnoAbierto--'23/02/2018'   
@CodEmp varchar (2),
@FechaTurno datetime   
as    
SELECT idAperturaCierre,FechaTurno,FechaCaja,HoraIni,CodResp,Nombres,MontoInicial,TotalPagos,TotalGastos,  
       TotalCajaParc,HoraCierre,EstadoCaja,EfectivoActual,Turno  
FROM AperturaCierreCaja  ACC
INNER JOIN dbo.Personal UP ON CodResp=Personal_ID
WHERE FechaTurno<=@FechaTurno 
  AND ACC.CodEmp=@CodEmp  
  AND EstadoCaja='A'  
    

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_ListaTurnoDia')
BEGIN
DROP PROC sp_ListaTurnoDia
 END
GO
CREATE   proc sp_ListaTurnoDia--'07/05/2017'   
@CodEmp varchar (2),
@FechaTurno datetime   
as    
SELECT ISNULL(MAX(Turno+1),'1')FROM AperturaCierreCaja    
WHERE CodEmp=@CodEmp
  and FechaTurno=@FechaTurno    
    

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_InsertTempMovimientoDia_Almacen')
BEGIN
DROP PROC sp_InsertTempMovimientoDia_Almacen
 END
GO
CREATE PROC sp_InsertTempMovimientoDia_Almacen    
    @FechaTurno DATETIME,       
    @CodEmp VARCHAR(2),  
    @idAperturaCierre INT,  
    @HoraIni VARCHAR(20),
    @Turno VARCHAR(2)  
AS    
BEGIN    
    SET NOCOUNT ON;
    SET XACT_ABORT ON; -- Revierte automáticamente la transacción si ocurre un error severo

    BEGIN TRY
        BEGIN TRANSACTION;

        DELETE FROM dbo.TempMovimientoDia_Almacen 
        WHERE CodEmp = @CodEmp    
          AND FechaMovimiento = @FechaTurno    
          AND idAperturaCierre = @idAperturaCierre;
             
        INSERT INTO dbo.TempMovimientoDia_Almacen (
            idAperturaCierre, 
            Producto_ID, 
            FechaMovimiento, 
            SaldoInicial,
            CantIngreso, 
            CantSalida, 
            SaldoFinal, 
            CodEmp, 
            Hora, 
            Turno
        )
        SELECT 
            @idAperturaCierre,
            Producto_ID,
            @FechaTurno,
            StockFinal,
            0,
            0,
            StockFinal,
            @CodEmp,
            @HoraIni,
            @Turno
        FROM dbo.Productos
        WHERE CodEmp = @CodEmp  
          AND EstadoProducto = 'Activo';
            
        COMMIT TRANSACTION;
        SELECT 0 AS Result; 
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
        BEGIN
		ROLLBACK TRANSACTION;
        END
        
        SELECT -1 AS Result;
    END CATCH
END

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_InsertAperturaCierreCaja')
BEGIN
DROP PROC sp_InsertAperturaCierreCaja
 END
GO
CREATE proc sp_InsertAperturaCierreCaja  
 @FechaTurno datetime,  
 @FechaCaja datetime,  
 @HoraIni varchar (21),  
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
 @HoraCierre varchar (20),  
 @EstadoCaja varchar (2),  
 @EfectivoActual money,  
 @Turno varchar (2),
 @CodEmp varchar (2),
 @UsuarioMod int  
as  
  
BEGIN    
    SET NOCOUNT ON;
    SET XACT_ABORT ON; -- Revierte automáticamente la transacción si ocurre un error severo

    BEGIN TRY
        BEGIN TRANSACTION;
  
INSERT INTO AperturaCierreCaja 
VALUES(@FechaTurno,@FechaCaja,@HoraIni,@CodResp,@MontoInicial,@VentaDia,@VentaContado,@VentaCredito,
       @VentaEfectivo,@VentaDeposito,@TotalPagos,@TotalGastos,@TotalCajaParc,@HoraCierre,@EstadoCaja,  
       @EfectivoActual,@Turno,@CodEmp,@UsuarioMod)  
  
DECLARE @idAperturaCierre INT

SELECT @idAperturaCierre=(SELECT MAX(idAperturaCierre)
                            FROM AperturaCierreCaja
                            WHERE CodEmp=@CodEmp
                              AND FechaTurno=@FechaTurno)
  
EXEC sp_InsertTempMovimientoDia_Almacen @FechaTurno,@CodEmp,@idAperturaCierre,@HoraIni,@Turno 
   
   COMMIT TRANSACTION
   SELECT 0 AS RESULT;
   
   END TRY
   BEGIN CATCH
   IF @@TRANCOUNT > 0
      BEGIN
      ROLLBACK TRAN
	  END

	  SELECT -1 AS RESULT;

	  END CATCH
END


GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_ListaMembresia')
BEGIN
DROP PROC sp_ListaMembresia
 END
GO
CREATE proc sp_ListaMembresia  
as  
select CodigoM,NombreMembresia,Duracion_Dias,Duracion_Semanas,Duracion_Meses,Duracion_Anios,
       DiasLimite,Frecuencia,Tarifa,Estado,UsuarioMod  
from Membresia    


GO
--ALTER TABLE [dbo].[DetRegistroPeriodoClases]ALTER COLUMN [NroRegistro]CHAR(8)NOT NULL
--GO
--ALTER TABLE [dbo].[RegistroPeriodoClases]ALTER COLUMN [NroRegistro]CHAR(8)NOT NULL
--GO

GO

--ALTER TABLE RegistroPeriodoClases ADD PRIMARY KEY (NroRegistro)
--GO

--ALTER TABLE DetRegistroPeriodoClases ADD FOREIGN KEY(NroRegistro)
--REFERENCES RegistroPeriodoClases

--GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_generaRegistroPeriodoClases')
BEGIN
DROP PROC sp_generaRegistroPeriodoClases
 END
GO
CREATE proc sp_generaRegistroPeriodoClases--'01'
@CodEmp varchar(2)
as

SELECT '0' + RIGHT('00000000' + CAST(ISNULL(CAST(RIGHT(MAX(NroRegistro), 8) AS INT), 0) + 1 AS VARCHAR(8)), 8)
FROM dbo.RegistroPeriodoClases
WHERE CodEmp = @CodEmp;

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_ListaRegistroPeriodoClasesXRegistro')
BEGIN
DROP PROC sp_ListaRegistroPeriodoClasesXRegistro
 END
GO
CREATE proc sp_ListaRegistroPeriodoClasesXRegistro--'00000002','40424328'
	@CodEmp varchar(2),
	@NroRegistro char(10)

as

SELECT [NroRegistro],[Pedido_ID],[Cliente_ID],[CodigoM],[FechaInicio],[FechaFin],[PersonalID],
       [SubTot],[IGV],[Total],[Estado],[Condicion],[Observacion],[CodEmp],[UsuarioMod] 
FROM dbo.RegistroPeriodoClases
WHERE CodEmp=@CodEmp
  and NroRegistro=@NroRegistro
    
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_InsertRegistroPeriodoClases')
BEGIN
DROP PROC sp_InsertRegistroPeriodoClases
 END
GO
CREATE PROCEDURE dbo.sp_InsertRegistroPeriodoClases
    @NroRegistro CHAR(10),
    @Pedido_ID CHAR(10),
    @Cliente_ID VARCHAR(20),
    @CodigoM VARCHAR(4),
    @FechaInicio DATETIME,
    @FechaFin DATETIME,
    @PersonalID INT,
    @SubTot MONEY,
    @IGV MONEY,
    @Total MONEY,
    @Estado CHAR(10),
    @Condicion VARCHAR(30),
    @Observacion VARCHAR(255),
    @CodEmp VARCHAR(2),
    @UsuarioMod INT
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;

    BEGIN TRY
        BEGIN TRANSACTION;

        IF NOT EXISTS (
            SELECT 1 
            FROM dbo.RegistroPeriodoClases WITH (UPDLOCK, HOLDLOCK)
            WHERE NroRegistro = @NroRegistro
              AND CodEmp = @CodEmp
        )
        BEGIN

            INSERT INTO dbo.RegistroPeriodoClases (
                NroRegistro, Pedido_ID, Cliente_ID, CodigoM, FechaInicio, FechaFin, 
                PersonalID, SubTot, IGV, Total, Estado, Condicion, Observacion, 
                CodEmp, UsuarioMod
            )
            VALUES (
                @NroRegistro, @Pedido_ID, @Cliente_ID, @CodigoM, @FechaInicio, @FechaFin, 
                @PersonalID, @SubTot, @IGV, @Total, @Estado, @Condicion, @Observacion, 
                @CodEmp, @UsuarioMod
            );
        END

        COMMIT TRANSACTION;
        SELECT 0 AS Result;
    END TRY
    BEGIN CATCH
        IF @@TRANCOUNT > 0
        BEGIN
            ROLLBACK TRANSACTION;
        END

        SELECT -1 AS Result;
    END CATCH
END

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_ListaCantMaximaAsistXPeriodoClasesXAlumno')
BEGIN
DROP PROC sp_ListaCantMaximaAsistXPeriodoClasesXAlumno
 END
GO
CREATE proc sp_ListaCantMaximaAsistXPeriodoClasesXAlumno--'01','40424328'  
@CodEmp varchar(2),  
@Cliente_ID varchar(20),  
@NroRegistro varchar(10)  
as  
  
IF EXISTS(SELECT 1  
   FROM DetRegistroPeriodoClases WITH(UPDLOCK,HOLDLOCK)
   WHERE Cliente_ID=@Cliente_ID  
     and NroRegistro=@NroRegistro) 
  
BEGIN  
select Dias=(Duracion_Dias),CANT=COUNT(IDSecuencia)  
from dbo.RegistroPeriodoClases rpc  
INNER JOIN dbo.Membresia M ON rpc.CodigoM=M.CodigoM  
INNER JOIN dbo.DetRegistroPeriodoClases drpc ON rpc.NroRegistro=drpc.NroRegistro  
WHERE CodEmp=@CodEmp  
  and rpc.Cliente_ID=@Cliente_ID  
  and rpc.NroRegistro=@NroRegistro  
GROUP BY Duracion_Dias  
  END  
ELSE  
BEGIN  
select Dias=(Duracion_Dias),CANT=0  
from dbo.RegistroPeriodoClases rpc  
INNER JOIN dbo.Membresia M ON rpc.CodigoM=M.CodigoM  
WHERE CodEmp=@CodEmp  
  and rpc.Cliente_ID=@Cliente_ID  
  and rpc.NroRegistro=@NroRegistro 

GROUP BY Duracion_Dias 
 
 END  
    

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_ListaDetRegistroPeriodoClasesXFecha')
BEGIN
DROP PROC sp_ListaDetRegistroPeriodoClasesXFecha
 END
GO
CREATE proc sp_ListaDetRegistroPeriodoClasesXFecha--'01','04/04/2018'      
 @CodEmp varchar(2),  
 @FechaIngreso datetime    
    
as    
    
SELECT Dia=(IDSecuencia),drpc.Cliente_ID,RazonSocial,HoraIngreso=CAST(HoraIngreso AS time),    
       FechaIngreso=CAST(FechaIngreso AS DATE),CondicionCredito 

FROM dbo.DetRegistroPeriodoClases drpc
INNER JOIN dbo.RegistroPeriodoClases rpc ON drpc.NroRegistro=rpc.NroRegistro
INNER JOIN dbo.Cliente cli ON drpc.Cliente_ID=cli.Cliente_ID 
INNER JOIN dbo.CtasXCobrar cxc ON drpc.Cliente_ID=cxc.Cliente_ID
      
  
WHERE cxc.CodEmp=@CodEmp 
  and FechaIngreso=@FechaIngreso   
  --and CondicionCredito='ADEUDA' 
  
UNION

SELECT Dia=(IDSecuencia),drpc.Cliente_ID,RazonSocial,HoraIngreso=CAST(HoraIngreso AS time),    
       FechaIngreso=CAST(FechaIngreso AS DATE),CondicionCredito='CANCELADO' 

FROM dbo.DetRegistroPeriodoClases drpc
INNER JOIN dbo.RegistroPeriodoClases rpc ON drpc.NroRegistro=rpc.NroRegistro 
INNER JOIN dbo.Cliente cli ON drpc.Cliente_ID=cli.Cliente_ID
  
WHERE rpc.CodEmp=@CodEmp 
  and FechaIngreso=@FechaIngreso   
  and drpc.Cliente_ID NOT IN(SELECT Cliente_ID FROM dbo.CtasXCobrar WHERE CodEmp=@CodEmp)  
  

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_ListaDetRegistroPeriodoClasesXFechaXTurno')
BEGIN
DROP PROC sp_ListaDetRegistroPeriodoClasesXFechaXTurno
 END
GO
CREATE proc sp_ListaDetRegistroPeriodoClasesXFechaXTurno--'00000001','40424341'      
 @CodEmp varchar(2),  
 @FechaIngreso datetime,  
 @Turno varchar(2)    
    
as    
    
SELECT Dia=(IDSecuencia),drpc.Cliente_ID,RazonSocial,CAST(HoraIngreso AS time),    
       FechaIngreso=CAST(FechaIngreso AS DATE),CondicionCredito 

FROM dbo.DetRegistroPeriodoClases drpc
INNER JOIN dbo.RegistroPeriodoClases rpc ON drpc.NroRegistro=rpc.NroRegistro
INNER JOIN dbo.Cliente cli ON drpc.Cliente_ID=cli.Cliente_ID 
INNER JOIN dbo.CtasXCobrar cxc ON drpc.Cliente_ID=cxc.Cliente_ID
      
  
WHERE cxc.CodEmp=@CodEmp 
  and FechaIngreso=@FechaIngreso    
  and Turno=@Turno  
   
UNION

SELECT Dia=(IDSecuencia),drpc.Cliente_ID,RazonSocial,CAST(HoraIngreso AS time),    
       FechaIngreso=CAST(FechaIngreso AS DATE),CondicionCredito='CANCELADO' 

FROM dbo.DetRegistroPeriodoClases drpc
INNER JOIN dbo.RegistroPeriodoClases rpc ON drpc.NroRegistro=rpc.NroRegistro 
INNER JOIN dbo.Cliente cli ON drpc.Cliente_ID=cli.Cliente_ID
  
WHERE rpc.CodEmp=@CodEmp 
  and FechaIngreso=@FechaIngreso
  and Turno=@Turno   
  and drpc.Cliente_ID NOT IN(SELECT Cliente_ID FROM dbo.CtasXCobrar WHERE CodEmp=@CodEmp) 
 


GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_ListaNroRegistroPeriodoClasesXAlumno')
BEGIN
DROP PROC sp_ListaNroRegistroPeriodoClasesXAlumno
 END
GO
CREATE proc sp_ListaNroRegistroPeriodoClasesXAlumno--'01','70012717'  
@CodEmp varchar(2),        
@Cliente_ID varchar(20)        
as        
        
select NroRegistro from dbo.RegistroPeriodoClases      
WHERE CodEmp=@CodEmp        
  and Cliente_ID=@Cliente_ID     
  and Estado<>'Anulada'       

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_ListaRegistroPeriodoClases')
BEGIN
DROP PROC sp_ListaRegistroPeriodoClases
 END
GO
CREATE proc sp_ListaRegistroPeriodoClases  
@CodEmp varchar(2),  
@Opc int  
as  

BEGIN

SET NOCOUNT ON;
SET XACT_ABORT ON;

BEGIN TRY  
BEGIN TRANSACTION;  
  
  
--IF(SELECT COUNT(*)   
--   FROM RegistroPeriodoClases   
--   WHERE CONVERT(CHAR(10),FechaFin,101)<=CONVERT(CHAR(10),GETDATE(),101))>0  
     
--BEGIN  
  
UPDATE RegistroPeriodoClases SET Condicion='TERMINADA'  
WHERE CAST(FechaFin AS DATE)<=(SELECT CAST(GETDATE() AS DATE))  
  and CodEmp=@CodEmp  
    
--  END  
--ELSE  
--  BEGIN  
UPDATE RegistroPeriodoClases SET Condicion='ACTIVA'  
WHERE CAST(FechaFin AS DATE)<=(SELECT CAST(GETDATE() AS DATE))  
  and CodEmp=@CodEmp  
   
  --END  
    
IF(@Opc=1)  
  
BEGIN  
SELECT rpc.NroRegistro,
       rpc.Pedido_ID,
       rpc.Cliente_ID AS CodSocio,
       cli.RazonSocial AS SocioCliente,
       rpc.CodigoM,  
       m.NombreMembresia AS Periodo,
       m.Duracion_Dias AS Duracion,
       rpc.FechaInicio,
       rpc.FechaFin,
       rpc.PersonalID,
       rpc.SubTot,
       rpc.IGV,
       rpc.Total,
       rpc.Estado,
       rpc.Condicion,
       rpc.Observacion  
FROM dbo.RegistroPeriodoClases rpc  
INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
WHERE rpc.CodEmp = @CodEmp  
  AND rpc.Condicion = 'ACTIVA'
    
UNION  
  
SELECT rpc.NroRegistro,
       rpc.Pedido_ID,
       rpc.Cliente_ID AS CodSocio,
       cli.RazonSocial AS SocioCliente,
       rpc.CodigoM,  
       m.NombreMembresia AS Periodo,
       m.Duracion_Dias AS Duracion,
       rpc.FechaInicio,
       rpc.FechaFin,
       rpc.PersonalID,
       rpc.SubTot,
       rpc.IGV,
       rpc.Total,
       rpc.Estado,
       rpc.Condicion,
       rpc.Observacion  
FROM dbo.RegistroPeriodoClases rpc  
INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
WHERE rpc.CodEmp = @CodEmp  
  and Condicion='TERMINADA'  
  and Pedido_ID in(SELECT NroRegistro FROM dbo.CtasXCobrar  
                     WHERE CodEmp=@CodEmp and CondicionCredito='ADEUDA')  
  
   END  
ELSE   
BEGIN  
SELECT rpc.NroRegistro,
       rpc.Pedido_ID,
       rpc.Cliente_ID AS CodSocio,
       cli.RazonSocial AS SocioCliente,
       rpc.CodigoM,  
       m.NombreMembresia AS Periodo,
       m.Duracion_Dias AS Duracion,
       rpc.FechaInicio,
       rpc.FechaFin,
       rpc.PersonalID,
       rpc.SubTot,
       rpc.IGV,
       rpc.Total,
       rpc.Estado,
       rpc.Condicion,
       rpc.Observacion  
FROM dbo.RegistroPeriodoClases rpc  
INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
WHERE rpc.CodEmp = @CodEmp  
  and Condicion='TERMINADA'   
  and Pedido_ID NOT IN(SELECT NroRegistro FROM dbo.CtasXCobrar WHERE CodEmp=@CodEmp)  
  and CAST(FechaFin AS DATE)<=(SELECT CAST(GETDATE() AS DATE))
    
UNION  
  
SELECT rpc.NroRegistro,
       rpc.Pedido_ID,
       rpc.Cliente_ID AS CodSocio,
       cli.RazonSocial AS SocioCliente,
       rpc.CodigoM,  
       m.NombreMembresia AS Periodo,
       m.Duracion_Dias AS Duracion,
       rpc.FechaInicio,
       rpc.FechaFin,
       rpc.PersonalID,
       rpc.SubTot,
       rpc.IGV,
       rpc.Total,
       rpc.Estado,
       rpc.Condicion,
       rpc.Observacion  
FROM dbo.RegistroPeriodoClases rpc  
INNER JOIN dbo.Cliente cli ON rpc.Cliente_ID = cli.Cliente_ID  
INNER JOIN dbo.Membresia m ON rpc.CodigoM = m.CodigoM  
WHERE rpc.CodEmp = @CodEmp  
  and Condicion='TERMINADA' 
  and Pedido_ID IN(SELECT NroRegistro FROM dbo.CtasXCobrar  
                     WHERE CodEmp=@CodEmp and CondicionCredito='CANCELADO')  
  and CAST(FechaFin AS DATE)<=(SELECT CAST(GETDATE() AS DATE)) 
    
ORDER BY NroRegistro ASC  
  
 END  
      COMMIT TRANSACTION
      SELECT 0 AS Result;
   END TRY
   BEGIN CATCH
      IF @@TRANCOUNT >0
	  BEGIN
	  ROLLBACK TRANSACTION
	    END
	  SELECT -1 AS Result
   END CATCH
 END

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Lista_EmpresaActiva')
BEGIN
DROP PROC sp_Lista_EmpresaActiva
 END
GO
CREATE  proc sp_Lista_EmpresaActiva  
as  
select Codigo_Emp,RazonSocial,RazonComercial 
from dbo.Empresa WHERE EstadoEmpresa='Activo'  
  
GO
ALTER TABLE [dbo].[Cliente]ALTER COLUMN [Cliente_ID]CHAR(8)
GO

-----------------------------------MANTENIMIENTO - ALTAS ----------------------------------------------------


IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_GeneraCodigoPersonal')
BEGIN
DROP PROC sp_GeneraCodigoPersonal
 END
GO
CREATE proc sp_GeneraCodigoPersonal
as
SELECT ISNULL(MAX([Personal_ID]+1),1) FROM [dbo].[Personal]
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Insert_UsuarioPersonal')
BEGIN
DROP PROC sp_Insert_UsuarioPersonal
 END
GO  
CREATE   proc sp_Insert_UsuarioPersonal
 @Personal_ID int ,
 @EstadoGene varchar(10),
 @ApellidoPaterno varchar(50),
 @ApellidoMaterno varchar(50),
 @Nombres varchar(50),
 @Direccion varchar(100),
 @Sexo varchar(10) ,
 @RucDNI varchar(12),
 @Telefono1 varchar(20),
 @Correo varchar(50),
 @Referencia varchar(50),
 @Area_ID char(3),

 @Usuario varchar(50),
 @Contraseña varchar(50),

 @CodEmp varchar(2),
 @Foto varchar(250),
 @UsuarioMod INT,
 @rol_id INT,
 @OpcUserSist int
 

as  
 BEGIN 
 

 INSERT INTO Auditoria_Sistema(usuario_id,tabla_afectada,accion,registro_id,detalle_cambio)
 VALUES(@UsuarioMod,'Personal','INSERT',@Personal_ID,'Se esta creando un nuevo personal en el Sistema')

 SET NOCOUNT ON
 SET XACT_ABORT ON
  DECLARE @usuario_id INT

  BEGIN TRY
  BEGIN TRANSACTION

  IF EXISTS(select 1  
   FROM Personal  
   WHERE Personal_ID=@Personal_ID)
  BEGIN  
  
  UPDATE UsuarioPersonal set EstadoGene=@EstadoGene,
                             ApellidoPaterno=@ApellidoPaterno,
                             ApellidoMaterno=@ApellidoMaterno,
                             Nombres=@Nombres,
                             Direccion=@Direccion,
                             Sexo=@Sexo,
                             RucDNI=@RucDNI,
                             Telefono1=@Telefono1,
                             Correo=@Correo,
                             Referencia=@Referencia,
                             Area_ID=@Area_ID,
                             CodEmp=@CodEmp,
                             Foto=@Foto
  where Personal_ID=@Personal_ID  

  IF(@OpcUserSist=1)
  BEGIN
  IF NOT EXISTS(SELECT 1 FROM Usuario WHERE Personal_ID=@Personal_ID)
     BEGIN 
	 SET @usuario_id=(SELECT ISNULL(MAX(usuario_id+1),1)FROM Usuario)

	 INSERT INTO Usuario([usuario_id],[personal_id],[Usuario],[Contraseña],[rol_id]) 
	 VALUES(@usuario_id,@Personal_ID,@Usuario,@Contraseña,@rol_id)
    END
  END

   INSERT INTO Auditoria_Sistema([fecha_hora],usuario_id,tabla_afectada,accion,registro_id,detalle_cambio)
   VALUES(GETDATE(),@UsuarioMod,'Personal','UPDATE',@Personal_ID,'Se esta modificando los datos del personal en el Sistema')

   END
  ELSE
  BEGIN
  INSERT INTO UsuarioPersonal(Personal_ID,EstadoGene,ApellidoPaterno,ApellidoMaterno,Nombres,Direccion,Sexo,RucDNI,Telefono1,
                              Correo,Referencia,Area_ID,CodEmp,Foto)   
  values(@Personal_ID,@EstadoGene,@ApellidoPaterno,@ApellidoMaterno,@Nombres,@Direccion,@Sexo,@RucDNI,@Telefono1,@Correo,
         @Referencia,@Area_ID,@Usuario,@Contraseña,@CodEmp,@Foto) 
		 
  IF(@OpcUserSist=1)
  BEGIN

  IF NOT EXISTS(SELECT 1 FROM Usuario WHERE Personal_ID=@Personal_ID)
     BEGIN 
	 SET @usuario_id=(SELECT ISNULL(MAX(usuario_id+1),1)FROM Usuario)

	 INSERT INTO Usuario([usuario_id],[personal_id],[Usuario],[Contraseña],[rol_id]) 
	 VALUES(@usuario_id,@Personal_ID,@Usuario,@Contraseña,@rol_id)
    END
   END

   INSERT INTO Auditoria_Sistema([fecha_hora],usuario_id,tabla_afectada,accion,registro_id,detalle_cambio)
   VALUES(GETDATE(),@UsuarioMod,'Personal','INSERT',@Personal_ID,'Se esta creando un nuevo personal en el Sistema')
  END  
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

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_autogenera_CodClientes')
BEGIN
DROP PROC sp_autogenera_CodClientes
 END
GO
CREATE PROCEDURE sp_autogenera_CodClientes  
AS  
BEGIN
    SET NOCOUNT ON;

    SELECT RIGHT('000000' + CAST(ISNULL(MAX(CAST(RIGHT(Cliente_ID, 6) AS INT)), 0) + 1 AS VARCHAR(6)), 6) 
    FROM Cliente;
END;

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Insert_Clientes')
BEGIN
DROP PROC sp_Insert_Clientes
 END
GO  
CREATE   proc [dbo].[sp_Insert_Clientes]  
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
 @HuellaDactilar VARBINARY(MAX)  
as  
 BEGIN TRANSACTION
 
  IF EXISTS(select 1  
   FROM cliente  
   WHERE Cliente_ID=@Cliente_ID)
  BEGIN   
  
  UPDATE cliente set RazonSocial=@RazonSocial,Direccion=@Direccion,Telefono=@Telefono,  
         Telefono2=@Telefono2,Correo=@Correo,RucDNI=@RucDNI,TipoId=@TipoId,  
         HuellaDactilar=@HuellaDactilar  
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

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_lista_clientes_Activos')
BEGIN
DROP PROC sp_lista_clientes_Activos
 END
GO
CREATE proc sp_lista_clientes_Activos    
as    
select Cliente_ID,RazonSocial,Direccion,Telefono,Estado,Telefono2,Correo,Sexo,RucDNI,TipoId,  
       Foto,Contacto,TelefonoCont, HuellaDactilar  
from cliente where Estado='Activo'    
order by RazonSocial asc  
  
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_lista_ClienteXcod')
BEGIN
DROP PROC sp_lista_ClienteXcod
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

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_lista_ClienteXRucDni')
BEGIN
DROP PROC sp_lista_ClienteXRucDni
 END
GO
CREATE  proc sp_lista_ClienteXRucDni --'000001'    
@RucDNI varchar(12)    
as    
select Cliente_ID,RazonSocial,Direccion,Telefono,Estado,Telefono2,Correo,Sexo,RucDNI,TipoId,  
       Foto,Contacto,TelefonoCont, HuellaDactilar  
from cliente  
where RucDNI=@RucDNI    
    
GO

  
IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Busq_ClienteXnombre')
BEGIN
DROP PROC sp_Busq_ClienteXnombre
 END
GO    
CREATE proc sp_Busq_ClienteXnombre --'JO'      
@razon varchar(100)      
as      
select Cliente_ID,RazonSocial,Direccion,Telefono,Estado,Telefono2,Correo,Sexo,RucDNI,TipoId,    
       Foto,Contacto,TelefonoCont,HuellaDactilar    
from cliente where Estado='Activo'     
 and razonsocial like '%'+@razon+'%'      
      
  
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Elimina_Cliente')
BEGIN
DROP PROC sp_Elimina_Cliente
 END
GO
CREATE proc sp_Elimina_Cliente  
@Cliente_ID char(11)  
as  

IF(SELECT COUNT(*)FROM RegistroPeriodoClases
   WHERE Cliente_ID=@Cliente_ID)>0
  BEGIN
  UPDATE Cliente SET [Estado]='Inactivo'WHERE Cliente_ID=@Cliente_ID
    END
  ELSE
IF(SELECT COUNT(*)FROM DetRegistroPeriodoClases
   WHERE Cliente_ID=@Cliente_ID)>0
  BEGIN
  UPDATE Cliente SET [Estado]='Inactivo'WHERE Cliente_ID=@Cliente_ID
  END
  ELSE
IF(SELECT COUNT(*)FROM CtasXCobrar
   WHERE Cliente_ID=@Cliente_ID)>0
  BEGIN
  UPDATE Cliente SET [Estado]='Inactivo'WHERE Cliente_ID=@Cliente_ID
  END
  ELSE
IF(SELECT COUNT(*)FROM Cabecera_Ventas
   WHERE Cliente_ID=@Cliente_ID)>0
  BEGIN
  UPDATE Cliente SET [Estado]='Inactivo'WHERE Cliente_ID=@Cliente_ID
    END
 ELSE
BEGIN
DELETE from dbo.Cliente where Cliente_ID=@Cliente_ID  
  END

GO


IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_elimina_personal')
BEGIN
DROP PROC sp_elimina_personal
 END
GO
CREATE proc sp_elimina_personal --'16'         
@Personal_ID int        
as        
      
IF(SELECT COUNT(*)FROM RegistroPeriodoClases
   WHERE PersonalID=@Personal_ID)>0
  BEGIN
  UPDATE Personal SET EstadoGene='Inactivo'WHERE Personal_ID=@Personal_ID
    END
  ELSE
IF(SELECT COUNT(*)FROM CtasXCobrar
   WHERE Personal_ID=@Personal_ID)>0
  BEGIN
  UPDATE Personal SET EstadoGene='Inactivo'WHERE Personal_ID=@Personal_ID
  END
  ELSE
IF(SELECT COUNT(*)FROM Cabecera_Ventas
   WHERE CodPer=@Personal_ID)>0
  BEGIN
  UPDATE Personal SET EstadoGene='Inactivo'WHERE Personal_ID=@Personal_ID
    END
 ELSE
BEGIN
DELETE FROM Personal WHERE Personal_ID=@Personal_ID                    
   END            
      
  
GO

ALTER TABLE [dbo].[Productos]DROP CONSTRAINT [FK_Producto2]
GO

ALTER TABLE [dbo].[Categoria]DROP CONSTRAINT [pk_Categoria]
GO
ALTER TABLE [Categoria] ALTER COLUMN [Categoria_ID]INT NOT NULL
GO

ALTER TABLE [Categoria] ADD PRIMARY KEY ([Categoria_ID])
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_max_cod_categoria')
BEGIN
DROP PROC sp_max_cod_categoria
 END
GO
CREATE proc sp_max_cod_categoria  
as  
select ISNULL(max(categoria_id+1),1) from categoria  
  
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Mante_Categorias')
BEGIN
DROP PROC sp_Mante_Categorias
 END
GO
CREATE proc sp_Mante_Categorias  
@Categoria_ID INT,
@Nombre varchar (50),  
@Estado varchar (10),
@rutaimagen varchar(150)  
as  

BEGIN
SET NOCOUNT ON
SET XACT_ABORT ON

  BEGIN TRY
    BEGIN TRANSACTION
	IF EXISTS (SELECT 1 FROM Categoria 
	            WHERE Categoria_ID = @Categoria_ID)
	BEGIN
	UPDATE Categoria SET Nombre=@Nombre,rutaimagen=@rutaimagen WHERE Categoria_ID=@Categoria_ID
	  END
	 ELSE
	BEGIN
	insert into Categoria(Categoria_ID,Nombre,Estado,rutaimagen) 
				  values(@Categoria_ID,@Nombre,@Estado,@rutaimagen) 
	  END
  COMMIT TRANSACTION
      RETURN 0
  END TRY
  BEGIN CATCH
  IF @@TRANCOUNT > 0
  BEGIN
  ROLLBACK TRANSACTION
    END
     RETURN - 1
  END CATCH
  END

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Elimina_Categorias')
BEGIN
DROP PROC sp_Elimina_Categorias
 END
GO
CREATE proc sp_Elimina_Categorias  
@Categoria_ID INT
as  

BEGIN
SET NOCOUNT ON
SET XACT_ABORT ON

  BEGIN TRY
    BEGIN TRANSACTION
	IF EXISTS (SELECT 1 FROM Productos 
	            WHERE Categoria_ID = @Categoria_ID)
	BEGIN
	UPDATE Categoria SET Estado='INACTIVO' WHERE Categoria_ID=@Categoria_ID
	  END
	 ELSE
	BEGIN
	DELETE FROM Categoria WHERE Categoria_ID = @Categoria_ID
	  END
  COMMIT TRANSACTION
      RETURN 0
  END TRY
  BEGIN CATCH
  IF @@TRANCOUNT > 0
  BEGIN
  ROLLBACK TRANSACTION
    END
     RETURN - 1
  END CATCH
  END

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_max_marc')
BEGIN
DROP PROC sp_max_marc
 END
GO
CREATE proc sp_max_marc    
as    
select ISNULL(max(MarcaProducto_ID+1),'1')from MarcaProducto    
    
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Mante_marcas')
BEGIN
DROP PROC sp_Mante_marcas
 END
GO
CREATE proc sp_Mante_marcas  
@Marca_ID int,  
@Nombre varchar (50)  ,  
@EstadoMarca varchar (10)  
  
as 
BEGIN
SET NOCOUNT ON
SET XACT_ABORT ON
 BEGIN TRY
   BEGIN TRANSACTION
   IF EXISTS(SELECT 1 
               FROM marcaproducto WHERE MarcaProducto_ID=@Marca_ID)
   BEGIN
   UPDATE marcaproducto 
      SET [Nombre]=@Nombre,
	      [EstadoMarca]=@EstadoMarca
   WHERE MarcaProducto_ID=@Marca_ID
     END
	ELSE
   BEGIN
   INSERT into marcaproducto(MarcaProducto_ID,Nombre,EstadoMarca) 
   values(@Marca_ID,@Nombre,@EstadoMarca)  
     END
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
  
IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Elim_MarcaPro')
BEGIN
DROP PROC sp_Elim_MarcaPro
 END
GO
CREATE proc sp_Elim_MarcaPro  
@Marca_ID int
  
as 
BEGIN
SET NOCOUNT ON
SET XACT_ABORT ON
 BEGIN TRY
   BEGIN TRANSACTION
   IF EXISTS (SELECT 1 FROM Productos 
	            WHERE MarcaProducto_ID = @Marca_ID)
	BEGIN
	UPDATE MarcaProducto SET EstadoMarca='INACTIVO' WHERE MarcaProducto_ID = @Marca_ID
	  END
	 ELSE
	BEGIN
	DELETE FROM MarcaProducto WHERE MarcaProducto_ID = @Marca_ID
	  END
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

ALTER TABLE [dbo].[Productos]ALTER COLUMN Imagen VARCHAR (150)
GO
  

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_autogenera_codprod')
BEGIN
DROP PROC sp_autogenera_codprod
 END
GO
CREATE PROCEDURE sp_autogenera_codprod  
AS  
BEGIN
    SET NOCOUNT ON;

    SELECT RIGHT('000000' + CAST(ISNULL(MAX(CAST(RIGHT(Producto_ID, 6) AS INT)), 0) + 1 AS VARCHAR(6)), 6) 
    FROM Productos;
END;

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Insert_Productos')
BEGIN
DROP PROC sp_Insert_Productos
 END
GO
CREATE proc sp_Insert_Productos-- '000012','vcx','cvx','1','cos','12/12/2005','ccg','hgv',12,12,10,20,5,'nkn'      
 @Producto_ID char(6),      
 @NombreProducto VARCHAR ( 50) ,      
 @EstadoProducto VARCHAR (10) ,      
 @Categoria_ID int ,      
 @MarcaProducto_ID int ,     
 @Descripcion VARCHAR ( 100),        
 @UnidadMedida VARCHAR (20),      
 @StockFinal real ,      
 @PrecioCosto MONEY ,      
 @PrecioVenta MONEY ,    
 @PrecioLimite MONEY ,    
 @MargenUtil MONEY ,      
 @Peso real,    
 @Imagen VARCHAR (150),    
 @CodigoBarra VARCHAR (50),    
 @CodEmp varchar(2),  
 @TipoProd varchar(2),
 @TipoBien_ID varchar(3)
       
as
BEGIN
SET NOCOUNT ON
SET XACT_ABORT ON
 BEGIN TRY
   BEGIN TRANSACTION

   INSERT INTO productos (Producto_ID,NombreProducto ,EstadoProducto  ,Categoria_ID ,      
                          MarcaProducto_ID,Descripcion,UnidadMedida ,StockFinal ,PrecioCosto ,PrecioVenta,PrecioLimite,      
                          MargenUtil,Peso,Imagen,CodigoBarra,CodEmp,TipoProd,TipoBien_ID) 

   VALUES(@Producto_ID,@NombreProducto ,@EstadoProducto  ,@Categoria_ID ,      
          @MarcaProducto_ID,@Descripcion,@UnidadMedida ,@StockFinal ,@PrecioCosto ,@PrecioVenta,@PrecioLimite,      
          @MargenUtil,@Peso,@Imagen,@CodigoBarra,@CodEmp,@TipoProd,@TipoBien_ID)

	COMMIT TRANSACTION
	  RETURN 0
	END TRY
	BEGIN CATCH
	IF @@TRANCOUNT>0
	BEGIN
	  ROLLBACK TRANSACTION
    END
	  RETURN -1
	END CATCH
	END

GO  

  
IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Update_Productos')
BEGIN
DROP PROC sp_Update_Productos
 END
GO   
CREATE  proc sp_Update_Productos      
 @Producto_ID char(6),      
 @NombreProducto VARCHAR ( 50) ,      
 @EstadoProducto VARCHAR (10) ,      
 @Categoria_ID int ,      
 @MarcaProducto_ID char(5),      
 @Descripcion VARCHAR ( 100),        
 @UnidadMedida VARCHAR (20),      
 @StockFinal real ,      
 @PrecioCosto MONEY ,      
 @PrecioVenta MONEY ,    
 @PrecioLimite MONEY ,    
 @MargenUtil MONEY ,      
 @Peso real,    
 @Imagen VARCHAR (150),    
 @CodigoBarra VARCHAR (50),    
 @CodEmp varchar(2),  
 @TipoProd varchar(2),
 @TipoBien_ID varchar(3)     
       
as      
BEGIN

SET NOCOUNT ON
SET XACT_ABORT ON

  BEGIN TRY
  BEGIN TRANSACTION
   update productos set NombreProducto=@NombreProducto ,EstadoProducto=@EstadoProducto,      
                        UnidadMedida=@UnidadMedida,StockFinal=@StockFinal,PrecioCosto=@PrecioCosto ,      
                        PrecioVenta=@PrecioVenta ,PrecioLimite=@PrecioLimite,MargenUtil=@MargenUtil,Peso=@Peso,    
                        Imagen=@Imagen,CodigoBarra=@CodigoBarra,TipoProd=@TipoProd,TipoBien_ID=@TipoBien_ID      
  where Producto_ID=@Producto_ID      
    and CodEmp=@CodEmp    
      
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
  
IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Elim_Producto')
BEGIN
DROP PROC sp_Elim_Producto
 END
GO
CREATE proc sp_Elim_Producto  
@Producto_ID char(6)
  
as 
BEGIN
SET NOCOUNT ON
SET XACT_ABORT ON
 BEGIN TRY
   BEGIN TRANSACTION
   IF EXISTS (SELECT 1 FROM Detalle_Ventas 
	            WHERE Producto_ID = @Producto_ID)
	BEGIN
	UPDATE Productos SET EstadoProducto='Inactivo' WHERE Producto_ID = @Producto_ID
	  END
	 ELSE
	BEGIN
	DELETE FROM Productos WHERE Producto_ID = @Producto_ID
	  END
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

--------------------------------------NORMALIZACION DE BASE DE DATOS ---------------------------------------
GO
ALTER TABLE [dbo].[Personal]DROP COLUMN [Usuario]
GO
ALTER TABLE [dbo].[Personal]DROP COLUMN [Contraseña]
GO
ALTER TABLE [dbo].[Personal]DROP COLUMN [UsuarioMod]
GO

IF NOT EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'U'AND NAME ='Rol')
BEGIN
CREATE TABLE Rol (
    rol_id INT PRIMARY KEY,
    nombre_rol VARCHAR(50) UNIQUE NOT NULL, -- Administrador, Cajero, Supervisor, Almacenero
    descripcion VARCHAR(255),               -- Qué puede hacer este rol a grandes rasgos
    fecha_creacion DATETIME DEFAULT CURRENT_TIMESTAMP
);
 END
GO

INSERT INTO Rol (rol_id,nombre_rol, descripcion) VALUES
(1,'Administrador', 'Acceso total al sistema, reportes financieros, gestión de personal y configuración.'),
(2,'Supervisor', 'Puede autorizar cancelaciones de ventas, aplicar descuentos especiales y ver inventarios.'),
(3,'Cajero', 'Operación exclusiva del punto de venta (POS), apertura/cierre de caja y emisión de comprobantes.'),
(4,'Almacenero', 'Gestión de stock, ingreso de mercadería, proveedores y control de inventario.');

GO

IF NOT EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'U'AND NAME ='Usuario')
BEGIN

CREATE TABLE Usuario (
    usuario_id INT PRIMARY KEY,
    personal_id INT UNIQUE, -- El puente que une al usuario con la persona
    Usuario VARCHAR(50) UNIQUE,
    Contraseña VARCHAR(255), -- Aquí se guarda la contraseña (encriptada)
    rol_id INT
);
END
GO

ALTER TABLE Usuario ADD FOREIGN KEY(personal_id)REFERENCES [Personal]
GO

INSERT INTO Usuario(usuario_id,personal_id,Usuario,Contraseña,rol_id)
SELECT 1,[Personal_ID],'MICMOM','995998339',1 FROM [dbo].[Personal]WHERE [Personal_ID]=1
GO

INSERT INTO Usuario(usuario_id,personal_id,Usuario,Contraseña,rol_id)
SELECT 2,[Personal_ID],'ADMIN','12345*',1 FROM [dbo].[Personal]WHERE [Personal_ID]=2
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_IngresoUsuarioSistema')
BEGIN
DROP PROC sp_IngresoUsuarioSistema
 END
GO
CREATE proc sp_IngresoUsuarioSistema
@Usuario varchar(50),
@Contraseña varchar(50)
as

select per.Personal_ID,ApellidoPaterno,ApellidoMaterno,Nombres,per.Area_ID,Area=(DescripcionArea),Rol=(nombre_rol),CodEmp
from Personal per
INNER JOIN Area ar ON per.Area_ID=ar.Area_ID
INNER JOIN Usuario U ON per.Personal_ID=U.Personal_ID
INNER JOIN Rol R ON U.rol_id=R.rol_id
where Usuario=@Usuario
  and Contraseña=@Contraseña

GO

CREATE TABLE Auditoria_Sistema (
    auditoria_id INT IDENTITY(1,1) PRIMARY KEY,
    fecha_hora DATETIME DEFAULT CURRENT_TIMESTAMP,
    
    usuario_id INT NOT NULL,      -- ¿Quién hizo la acción?
    tabla_afectada VARCHAR(50),   -- 'Personal', 'Ventas', 'Productos'
    accion VARCHAR(20),           -- 'INSERT', 'UPDATE', 'DELETE'
    registro_id VARCHAR(10),      -- El ID de La tabla modificada(Personal, Clientes, productos,ventas etc)
    detalle_cambio TEXT,          -- Descripción con los datos viejos y nuevos
    
    FOREIGN KEY (usuario_id) REFERENCES Usuario(usuario_id)
);
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_ListaRoles')
BEGIN
DROP PROC sp_ListaRoles
 END
GO
CREATE proc sp_ListaRoles
as
select * from Rol
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_ListaTipo_Bien')
BEGIN
DROP PROC sp_ListaTipo_Bien
 END
GO
CREATE proc sp_ListaTipo_Bien
as
select * from Tipo_Bien
GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Lista_ProductosGral_Activos')
BEGIN
DROP PROC sp_Lista_ProductosGral_Activos
 END
GO
CREATE PROC sp_Lista_ProductosGral_Activos      
    @CodEmp VARCHAR(2)    
AS      
BEGIN
    SET NOCOUNT ON; -- Reduce drásticamente el tráfico de red al omitir mensajes de filas afectadas

    SELECT 
        p.Producto_ID,
        p.Descripcion,
        p.EstadoProducto,
        Categoria = c.nombre,
        Marca = m.nombre,    
        p.NombreProducto,
        p.UnidadMedida,
        p.StockFinal,
        p.PrecioCosto,
        p.PrecioVenta,
        p.PrecioLimite,    
        p.MargenUtil,
        Peso = ISNULL(p.Peso, 0),
        p.Imagen,
        p.CodigoBarra,
        p.TipoProd     
    FROM dbo.productos AS p WITH (NOLOCK) -- Recomendado para consultas masivas de lectura en reportería rápida
    INNER JOIN dbo.marcaproducto AS m ON m.MarcaProducto_ID = p.MarcaProducto_ID    
    INNER JOIN dbo.categoria AS c ON c.Categoria_ID = p.Categoria_ID    
    WHERE p.EstadoProducto = 'Activo'     
      AND p.CodEmp = @CodEmp    
    ORDER BY p.Descripcion ASC, p.NombreProducto ASC;
END;

GO

IF EXISTS(SELECT * FROM SYSOBJECTS WHERE TYPE = 'P'AND NAME ='sp_Lista_ProductosGral_ActivosXNom')
BEGIN
DROP PROC sp_Lista_ProductosGral_ActivosXNom
 END
GO
CREATE PROC sp_Lista_ProductosGral_ActivosXNom
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
 */

