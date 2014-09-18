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

    public static void main(String[] args) {
        (new primaryThread())       .start();
    }
}

class primaryThread extends Thread {
    
    int   clock         = 0;
    int[] datos         = new int[200];
    int[] instrucciones = new int[400];
    
    public void primaryThread(){
        for(int i = 0; i <= 200; i++) {
        datos[i] = 0;
        }
        
        for (int i = 0; i <= 400; i++) {
        instrucciones[i] = 0;
        }
        
    }

    public void run(){
        (new instructionFetch())    .start();
        (new instructionDecode())   .start();
        (new execute())             .start();
        (new memory())              .start();
        (new writeBack())           .start();
    }
    
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

class instructionFetch extends Thread {
    public void run(){
        System.out.println("IF");
    }
}

class instructionDecode extends Thread {
    public void run(){
        System.out.println("ID");
    }
}

class execute extends Thread {
    public void run(){
        System.out.println("EX");
    }
}

class memory extends Thread {
    public void run(){
        System.out.println("MEM");
    }
}

class writeBack extends Thread {
    public void run(){
        System.out.println("WB");
    }
}

