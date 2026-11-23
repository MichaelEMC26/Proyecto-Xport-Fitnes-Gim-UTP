-- Copia de SisdataGimnasio/Script/AsistenciaHora.sql (mismo script que usa el WinForm).
-- Ejecutar en la base de datos de la web si "Asistencias del periodo" no muestra la hora:
-- la tabla DetRegistroAsistencias guarda HoraIngreso como timestamp/rowversion y la hora real
-- se guarda en dbo.AsistenciaHora (sp_AsistenciaHora_Grabar / sp_AsistenciaHora_Lista).


/* =====================================================================
   Hora de ingreso de las asistencias (frm_RegistrarAsistencia)

   La columna HoraIngreso de la tabla de asistencias es de tipo "timestamp"
   (rowversion): llega al sistema como byte[8] y no guarda la hora.
   Para no tocar esa tabla ni sp_InsertDetRegistroAsistencias, la hora de
   cada ingreso se guarda aparte en dbo.AsistenciaHora (una fila por
   suscripcion, socio y dia: el sistema no permite dos ingresos el mismo dia).

   Lo usan: Ven_FacturaN.RegistrarAsistenciaN (graba la hora despues del
   ingreso) y Ven_FacturaN.CompletarHorasAsistencia (la pone en la lista).
   Se puede ejecutar varias veces.
   ===================================================================== */
SET ANSI_NULLS ON
SET QUOTED_IDENTIFIER ON
GO

IF OBJECT_ID('dbo.AsistenciaHora', 'U') IS NULL
BEGIN
    CREATE TABLE dbo.AsistenciaHora
    (
        NroSuscripcion_id varchar(10) NOT NULL,
        Cliente_ID        varchar(20) NOT NULL,
        FechaIngreso      date        NOT NULL,
        HoraIngreso       datetime    NOT NULL,
        CONSTRAINT PK_AsistenciaHora PRIMARY KEY (NroSuscripcion_id, Cliente_ID, FechaIngreso)
    )
END
GO

IF OBJECT_ID('dbo.sp_AsistenciaHora_Grabar', 'P') IS NOT NULL
    DROP PROCEDURE dbo.sp_AsistenciaHora_Grabar
GO
/* Guarda la hora del ingreso del dia; si ya existe para ese dia no la cambia */
CREATE PROCEDURE dbo.sp_AsistenciaHora_Grabar
    @NroSuscripcion_id varchar(10),
    @Cliente_ID        varchar(20),
    @FechaHora         datetime
AS
BEGIN
    SET NOCOUNT ON

    DECLARE @Nro varchar(10) = LTRIM(RTRIM(@NroSuscripcion_id)),
            @Cli varchar(20) = LTRIM(RTRIM(@Cliente_ID)),
            @Dia date        = CAST(@FechaHora AS date)

    IF NOT EXISTS (SELECT 1 FROM dbo.AsistenciaHora
                   WHERE NroSuscripcion_id = @Nro AND Cliente_ID = @Cli AND FechaIngreso = @Dia)
        INSERT INTO dbo.AsistenciaHora (NroSuscripcion_id, Cliente_ID, FechaIngreso, HoraIngreso)
        VALUES (@Nro, @Cli, @Dia, @FechaHora)
END
GO

IF OBJECT_ID('dbo.sp_AsistenciaHora_Lista', 'P') IS NOT NULL
    DROP PROCEDURE dbo.sp_AsistenciaHora_Lista
GO
/* Horas de ingreso de una suscripcion */
CREATE PROCEDURE dbo.sp_AsistenciaHora_Lista
    @NroSuscripcion_id varchar(10),
    @Cliente_ID        varchar(20)
AS
BEGIN
    SET NOCOUNT ON

    SELECT FechaIngreso, HoraIngreso
    FROM dbo.AsistenciaHora
    WHERE NroSuscripcion_id = LTRIM(RTRIM(@NroSuscripcion_id))
      AND Cliente_ID = LTRIM(RTRIM(@Cliente_ID))
    ORDER BY FechaIngreso
END
GO
