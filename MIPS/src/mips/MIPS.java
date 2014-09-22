/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mips;
import java.lang.Thread;
import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;
/**
 *
 * @author JoseSlon
 */
public class MIPS {

    static int     clock           = 0;
    static int[]   datos           = new int[200];
    static int[]   instrucciones   = new int[400];
    static int[]   registros       = new int[32];
    static int[]   instruccionIF   = new int[4];
    static int[]   instruccionID   = new int[4];
    static int[]   instruccionEX   = new int[4];
    static int[]   instruccionMEM  = new int[4];
    static int[]   instruccionWB   = new int[4];
    static int     pc              = 0;
    static int[]   tablaReg        = new int[32]; //para que a ana no se le olvide que es para identificar conflictos
    static int[]   tablaEtapa      = new int[5];  //vector de banderas donde indican la finalización de cada etapa
    
    
    public static void main(String[] args) {
        
        
        for(int i = 0; i < 200; i++) {
        datos[i] = 0;
        }
        
        for (int i = 0; i < 400; i++) {
        instrucciones[i] = 0;
        }
        
        for(int i = 0 ; i < 32; i++) {
                tablaReg[i] = 0;
        }
        
        //Hilos
    
     final Runnable instructionFetch = new Runnable(){
            public void run(){
                tablaEtapa[0] = 1; //indica que acaba de iniciar
                //System.out.println("IF");
                //System.out.println("El vector de if queda: ");
                for(int i = 0; i < 4 ; i++) {
                    instruccionIF[i] = instrucciones[(pc*4)+i];
                    //System.out.println(instruccionIF[i]);
                    
                }
                
                //******** esto es momentaneo mientras averiguamos como usar semaforos***************
                while(tablaEtapa[1] == 1) {     //mientras ID esté ocupado
                    //Thread.yield();
                }
                for(int i =0; i < 4; i++){
                    
                    instruccionID[i] = instruccionIF[i];
                }
                tablaEtapa[0] = 0; //indica que terminó
            }
        
        };
        
        final Runnable instructionDecode = new Runnable(){
            public void run(){
                tablaEtapa[1] = 1; //indica que acaba de iniciar
                int opCode  = instruccionID[0];
                int op1     = instruccionID[1];
                int op2     = instruccionID[2];
                int op3     = instruccionID[3];
                
                System.out.println("Instruction ID:");
                for(int i = 0; i < 4; i++){
                    System.out.print(instruccionID[i]);
                }
                tablaEtapa[1] = 0; //indica que terminó

                //switch que identifique el OP
                switch(opCode){
                    case 8: {                   //DADDI
                        if(tablaReg[op1] == 0 && tablaReg[op2] == 0 ){  //Si los registros op1 y op2 están libres
                            if(tablaEtapa[2] == 0){             //Si EX está desocupado
                                for(int i =0; i < 4; i++){
                                    instruccionEX[i] = instruccionID[i];
                                }
                            }
                            else{           //Si EX está ocupado
                                //wait();
                            }
                        }
                    }
                    case 32: {                  //DADD
                    
                    
                    }
                    case 34: {                  //DSUB
                    
                    }
                    case 12: {                  //DMUL
                    
                    }
                    case 14: {                  //DDIV
                    
                    }
                    case 35: {                  //LW
                    
                    }
                    case 43: {                  //SW
                    
                    }
                    case 4:  {                  //BEQZ
                    
                    }
                    case 5:  {                  //BNEZ
                    
                    }
                    case 3:  {                  //JAL
                    
                    }
                    case 2:  {                  //JR
                    
                    }
                    case 63: {                  //FIN
                    
                    }
                }
                
            }
        };
        
        final Runnable execute = new Runnable(){
            public void run(){
                tablaEtapa[2] = 1; //indica que acaba de iniciar
                tablaEtapa[2] = 0; //indica que terminó
                System.out.println("EX");
            }
        };
        
        final Runnable memory = new Runnable(){
            public void run(){
                tablaEtapa[3] = 1; //indica que acaba de iniciar
                tablaEtapa[3] = 0; //indica que terminó
                System.out.println("MEM");
            }
        };
        
        final Runnable writeBack = new Runnable(){
            public void run(){
                tablaEtapa[4] = 1; //indica que acaba de iniciar
                tablaEtapa[4] = 0; //indica que terminó
                System.out.println("WB");
            }
        };
        
        Runnable principalThread = new Runnable(){
            public void run(){
                MIPS mips = new MIPS();
                mips.cargarInstrucciones();
                
                for(int i = 0; i < mips.instrucciones.length; i++ ) {
                  System.out.println(Integer.toString(mips.instrucciones[i]));
                }
                
                new Thread(instructionFetch).start();
                new Thread(instructionDecode).start();
                new Thread(execute).start();
                new Thread(memory).start();
                new Thread(writeBack).start();
            }
        };
        new Thread(principalThread).start();
    }      
    
    
    
    
    //Operaciones 
    
    int daddi(int ry, int rx, int n){
         int resultado = -1;
        return resultado;
    }
    
    int dadd(int ry, int rz, int rx){
         int resultado = -1;
        return resultado;
    }
    
    int dsub(int ry, int rz, int rx){
         int resultado = -1;
        return resultado;
    }
    
    int dmul(int ry, int rz, int rx){
         int resultado = -1;
        return resultado;
    }
    
    int ddiv(int ry, int rz, int rx){
         int resultado = -1;
        return resultado;
    }
    
    int lw(int ry, int rx, int n){
         int resultado = -1;
        return resultado;
    }
    
    int sw(int ry, int rx, int n){
         int resultado = -1;
        return resultado;
    }
    
    int bnez(int rx, int label){
         int resultado = -1;
        return resultado;
    }
    
    int beqz(int rx, int label){
         int resultado = -1;
        return resultado;
    }
    
    int jal(int n){
         int resultado = -1;
        return resultado;
    }
    
    int jr(int rx){
         int resultado = -1;
        return resultado;
    }
    

    
    void cargarInstrucciones(){
        try {
            BufferedReader bf = new BufferedReader(new FileReader("HILO-1.txt"));
            String linea = "";
            int i = 0;
            while ((linea = bf.readLine()) != null) {
                String[] parts = linea.split("\\s");
                for(String part : parts){
                    instrucciones[i] = Integer.valueOf(part);
                    i++;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
        }      
    }
    
    
}
