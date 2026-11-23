-- Dashboard: "Cumpleaños del día".
-- La versión anterior comparaba FechaCumpleaños = @FechaActual (incluye el año), por lo que solo
-- encontraba socios nacidos HOY. Se compara solo mes y día, y solo socios activos.
CREATE OR ALTER PROCEDURE sp_MuestraFechaCumpleañosActualSocio
    @FechaActual DATE
AS
BEGIN
    SET NOCOUNT ON;
    SELECT [Cliente_ID], [RazonSocial], [Telefono], [Telefono2], [Correo], [Contacto], [TelefonoCont], FechaCumpleaños
      FROM [dbo].[Cliente]
     WHERE FechaCumpleaños IS NOT NULL
       AND MONTH(FechaCumpleaños) = MONTH(@FechaActual)
       AND DAY(FechaCumpleaños)   = DAY(@FechaActual)
       AND Estado = 'Activo'
     ORDER BY RazonSocial;
END
GO
