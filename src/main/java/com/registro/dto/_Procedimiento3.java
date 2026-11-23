/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dto;

/**
 *
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
 @TotalIngEfectivo money,  
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
          @VentaEfectivo, @VentaDeposito, @TotalPagos, @TotalGastos, @TotalIngEfectivo, @HoraCierre, @EstadoCaja,  
          @EfectivoActual, @Turno, @CodEmp, @UsuarioMod)  
  
  --DECLARE @idAperturaCierre int  
  --SELECT @idAperturaCierre = (SELECT MAX(idAperturaCierre) FROM AperturaCierreCaja  
  --                            WHERE CodEmp = @CodEmp AND FechaTurno = @FechaTurno)  
  
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

-- Movimiento de Caja: monto cobrado a cuenta entre fechas y por turno  
ALTER   PROCEDURE sp_ListaAcuentaPagoXFechasXTurno -- '01','13/03/2018','13/03/2018','2'  
@CodEmp varchar(2),  
@FechaIni datetime,  
@FechaFin datetime,  
@Turno varchar(2)  
AS  
SELECT AcuentaPago = SUM(ISNULL(dcxc.ACuenta, 0))  
FROM dbo.DetalleCtasXCobrar dcxc  
INNER JOIN dbo.CtasXCobrar cxc ON dcxc.Cobro_ID = cxc.Cobro_ID  
INNER JOIN dbo.AperturaCierreCaja acc ON dcxc.UsuarioMod = acc.UsuarioMod  
WHERE dcxc.FechaAmortizacion BETWEEN @FechaIni AND @FechaFin  
  AND acc.Turno = @Turno  
  AND cxc.CodEmp = @CodEmp  

GO

    
ALTER  proc sp_ListaAperturaCierreCajaAbierto--'01','01'            
@CodEmp varchar(2)           
              
as              
select [idAperturaCierre],[FechaTurno],[FechaCaja],[HoraIni],[CodResp],[MontoInicial],[VentaDia],  
    [VentaContado],[VentaCredito],[VentaEfectivo],[VentaDeposito],[TotalPagos],[TotalGastos],  
    [TotalIngEfectivo],[HoraCierre],[EstadoCaja],[EfectivoActual],[Turno],[CodEmp]  
from dbo.AperturaCierreCaja where CodEmp=@CodEmp and EstadoCaja='A'        
    
  
GO
  
ALTER  proc sp_ListaAperturaCierreCajaXFechaTurno--'01','14/03/2016'      
@CodEmp varchar(2),      
@FechaTurno datetime      
      
as      
      
select CodResp,Nombres,Turno,NomTurno=' '+(CASE Turno WHEN '1'THEN '1ER'      
      WHEN '2'THEN '2DO'      
      WHEN '3'THEN '3ER'      
      WHEN '4'THEN '4TO'      
      WHEN '5'THEN '5TO'      
      WHEN '6'THEN '6TO'      
      WHEN '7'THEN '7MO'      
      WHEN '8'THEN '8VO'      
             ELSE'9NO' END)+' '+'TURNO'+' '+Nombres      
from dbo.AperturaCierreCaja acc
INNER JOIN dbo.Personal p ON CodResp=Personal_ID     
where acc.CodEmp=@CodEmp      
  and FechaTurno=@FechaTurno      
      

GO

  
ALTER   proc sp_ListaCobrosDiaXTurno-- '24/02/2018',1,'01'  
@FechaTurno datetime,      
@idAperturaCierre int,      
@CodEmp varchar(2)     
as      
      
select Monto=cast(isnull(sum(ACuenta),0)as decimal(10,2))
from dbo.DetalleCtasXCobrar dcxc
INNER JOIN dbo.CtasXCobrar cxc ON dcxc.Cobro_ID=cxc.Cobro_ID
INNER JOIN dbo.AperturaCierreCaja acc ON dcxc.UsuarioMod=acc.UsuarioMod
 
where idAperturaCierre=@idAperturaCierre  
  and FechaAmortizacion=@FechaTurno  
  and acc.CodEmp=@CodEmp  
      

GO
  
ALTER proc sp_ListaCtasXCobrarAmortizadosXRangoFechasXTurno  
@CodEmp varchar(2),  
@FechaIni datetime,  
@FechaFin datetime,  
@Turno varchar(2)  
  
as  
select Cobro_ID,cxc.Cliente_ID,cte.RazonSocial,NroRegistro,FechaEmi,TipoDocumento,NroDocumento,Monto,NroDias,Personal_ID,  
       SaldoCredito,CondicionCredito,FechaVenc  
from dbo.CtasXCobrar cxc
INNER JOIN dbo.Cliente cte ON cxc.Cliente_ID=cte.Cliente_ID
where CodEmp=@CodEmp  
  and Cobro_ID IN(select Cobro_ID from dbo.DetalleCtasXCobrar dcxc
                  INNER JOIN dbo.AperturaCierreCaja acc ON dcxc.UsuarioMod=acc.UsuarioMod 
                  where FechaAmortizacion=FechaTurno  
                    and FechaAmortizacion BETWEEN @FechaIni and @FechaFin  
                    and Turno=@Turno)  
GO

  
ALTER   proc sp_ListaGastosDiaXTurno      
@FechaTurno datetime,      
@idAperturaCierre int,      
@CodEmp varchar(2)     
as      
      
select Monto=cast(isnull(sum(ImporteGasto),0)as decimal(10,2))
from dbo.GastosDiversos gd 
INNER JOIN dbo.AperturaCierreCaja acc ON gd.UsuarioMod=acc.UsuarioMod    
WHERE idAperturaCierre=@idAperturaCierre  
  and gd.FechaTurno=@FechaTurno  
  and gd.CodEmp=@CodEmp  
      
GO

ALTER   proc sp_ListaTurnoAbierto--'23/02/2018'     
@CodEmp varchar (2),  
@FechaTurno datetime     
as      
SELECT idAperturaCierre,FechaTurno,FechaCaja,HoraIni,CodResp,Nombres,MontoInicial,TotalPagos,TotalGastos,    
       TotalIngEfectivo,HoraCierre,EstadoCaja,EfectivoActual,Turno    
FROM AperturaCierreCaja  ACC  
INNER JOIN dbo.Personal UP ON CodResp=Personal_ID  
WHERE FechaTurno<=@FechaTurno   
  AND ACC.CodEmp=@CodEmp    
  AND EstadoCaja='A'    
      
  
GO
  
ALTER   proc sp_ListaTurnoDia--'07/05/2017'     
@CodEmp varchar (2),  
@FechaTurno datetime     
as      
SELECT Turno=ISNULL(MAX(Turno+1),'1')FROM AperturaCierreCaja      
WHERE CodEmp=@CodEmp  
  and FechaTurno=@FechaTurno      
      

GO

-- sp_UpdateAperturaCierreCaja  
ALTER   PROCEDURE sp_UpdateAperturaCierreCaja   
 @idAperturaCierre INT,   
 @FechaTurno datetime,    
 @FechaCaja datetime,    
 --@HoraIni varchar (21),    
 @CodResp int,    
 @MontoInicial money,    
 @VentaDia money,  
 @VentaContado money,  
 @VentaCredito money,  
 @VentaEfectivo money,  
 @VentaDeposito money,  
 @TotalPagos money,    
 @TotalGastos money,    
 @TotalIngEfectivo money,    
 @HoraCierre varchar (20),    
 @EstadoCaja varchar (2),    
 @EfectivoActual money,    
 @Turno varchar (2),  
 @CodEmp varchar (2),  
 @UsuarioMod int      
as    

BEGIN
SET NOCOUNT ON
SET XACT_ABORT ON

  BEGIN TRY
  BEGIN TRANSACTION    
    
 IF EXISTS (SELECT 1          
       FROM AperturaCierreCaja          
       WHERE idAperturaCierre=@idAperturaCierre          
         AND CodEmp=@CodEmp                
         AND FechaTurno=@FechaTurno          
         AND Turno=@Turno)        
 BEGIN          
        
--EXEC sp_UpdateTempMovimientoDia_Almacen @FechaTurno,@CodEmp,@Turno    
  
UPDATE AperturaCierreCaja   
SET FechaCaja=@FechaCaja,MontoInicial=@MontoInicial,VentaDia=@VentaDia,  
    VentaContado=@VentaContado,VentaCredito=@VentaCredito,VentaEfectivo=@VentaEfectivo,  
    VentaDeposito=@VentaDeposito,TotalPagos=@TotalPagos,TotalGastos=@TotalGastos,  
    TotalIngEfectivo=@TotalIngEfectivo,HoraCierre=@HoraCierre,EstadoCaja=@EstadoCaja,    
    EfectivoActual=@EfectivoActual,Turno=@Turno,UsuarioMod=@UsuarioMod    
WHERE idAperturaCierre=@idAperturaCierre  
  
 UPDATE dbo.Cabecera_Ventas SET Dia='C'    
   WHERE CodEmp=@CodEmp            
     AND FechaTurno=@FechaTurno     
     AND Turno=@Turno      
       
  UPDATE dbo.Registro_de_Compra SET Dia='C'    
   WHERE CodEmp=@CodEmp            
     AND FechaIngreso=@FechaTurno     
     AND Turno=@Turno     
       
  UPDATE dbo.GastosDiversos SET Dia='C'    
   WHERE CodEmp=@CodEmp            
     AND FechaTurno=@FechaTurno     
     AND Turno=@Turno           
    
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

ALTER PROCEDURE [dbo].[sp_Reporte_TicketCierreCaja]
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
 */
public class _Procedimiento3 {
    
}
