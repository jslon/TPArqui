/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package mips;
import java.lang.Thread;
/**
 *
 * @author JoseSlon
 */
public class MIPS {

    int     clock         = 0;
    int[]   datos         = new int[200];
    int[]   instrucciones = new int[400];
    int[]   registros     = new int[32];
    int[]   instruccion   = new int[4];
    int     pc            = 0;
    int[][] tablaReg      = new int[32][2]; //para que a ana no se le olvide que es para identificar conflictos
    
    
    
    public static void main(String[] args) {
        MIPS mips = new MIPS();
        
        for(int i = 0; i <= 200; i++) {
        mips.datos[i] = 0;
        }
        
        for (int i = 0; i <= 400; i++) {
        mips.instrucciones[i] = 0;
        }
        
        //new Thread(instructionFetch).start();
        
        Runnable instructionFetch = new Runnable(){
            public void run(){
                //System.out.println("IF");
                for(int i = 0; i <4 ; i++) {
                    //instruccion[i] = instrucciones[(pc*4)+i];
                    // copiamos los valores del vector de instrucciones al vector tamano jenny
                }
                
            }
        
        };
        
        Runnable instructionDecode = new Runnable(){
            public void run(){
                System.out.println("ID");
                //switch que identifique el OP
                
            }
        };
        
        Runnable execute = new Runnable(){
            public void run(){
                System.out.println("EX");
            }
        };
        
        Runnable memory = new Runnable(){
            public void run(){
                System.out.println("MEM");
            }
        };
        
        Runnable writeBack = new Runnable(){
            public void run(){
                System.out.println("WB");
            }
        };       
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
    
    
    
    boolean conflicto(){
    
    }
    
    
    
    
}
