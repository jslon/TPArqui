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

    int   clock         = 0;
    int[] datos         = new int[200];
    int[] instrucciones = new int[400];
    
    public static void main(String[] args) {
        MIPS mips = new MIPS();
        
        for(int i = 0; i <= 200; i++) {
        mips.datos[i] = 0;
        }
        
        for (int i = 0; i <= 400; i++) {
        mips.instrucciones[i] = 0;
        }
        //(new primaryThread()).start();
        
        Runnable instructionFetch = new Runnable(){
            public void run(){
            
            }
        
        };
        
        Runnable instructionDecode = new Runnable(){
            public void run(){
            
            }
        };
        
        Runnable execute = new Runnable(){
            public void run(){
            
            }
        };
        
        Runnable memory = new Runnable(){
            public void run(){
            
            }
        };
        
        Runnable writeBack = new Runnable(){
            public void run(){
            
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
}
