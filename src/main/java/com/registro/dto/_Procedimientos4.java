/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/Classes/Class.java to edit this template
 */
package com.registro.dto;

/**
 *
CREATE  proc sp_ListaTarjetasPago           
as          
select * from TarjetasPago          
  
GO

CREATE  proc sp_ListaTarjetasPagoActivo           
as          
select * from TarjetasPago WHERE [Estado]='ACTIVO'         
  
GO

ALTER  proc sp_ListaTarjetasPagoXCodPago        
@IDFormaPago varchar (2)        
as        
select * from TarjetasPago where IDFormaPago=@IDFormaPago        

GO

ALTER  proc sp_ListaTarjetasPagoXCod 
@IDTarjeta varchar (3)        
as        
select * from TarjetasPago where IDTarjeta=@IDTarjeta     
 

GO
    
CREATE  proc sp_InsertTarjetasPago        
 @IDTarjeta varchar (3),        
 @NombreTarjetaPago varchar (100),        
 @Estado varchar (10),      
 @PorCom real,
 @IDFormaPago varchar (2) 
         
as        
BEGIN TRANSACTION        
INSERT into TarjetasPago values(@IDTarjeta,@NombreTarjetaPago,@Estado,@PorCom,@IDFormaPago)        
        
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

    
CREATE  proc sp_UpdateTarjetasPago          
 @IDTarjeta varchar (3),          
 @NombreTarjetaPago varchar (100),          
 @Estado varchar (10),        
 @PorCom real,
 @IDFormaPago varchar (2)          
           
as          
BEGIN TRANSACTION          
UPDATE TarjetasPago SET NombreTarjetaPago=@NombreTarjetaPago,Estado=@Estado,PorCom=@PorCom,IDFormaPago=@IDFormaPago          
WHERE IDTarjeta=@IDTarjeta          
          
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
* 
* 
CREATE proc sp_GeneraCodigoTarjeta
as
SELECT RIGHT('00' + CAST(ISNULL(MAX(CAST(IDTarjeta AS INT)), 0) + 1 AS VARCHAR(2)), 2) 
FROM TarjetasPago;
GO

 */
public class _Procedimientos4 {
    
}
