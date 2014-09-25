/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mips;

import java.lang.Thread;
import java.io.*;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author JoseSlon
 */
public class MIPS {

    static int clock = 0;
    static int[] datos = new int[200];
    static int[] instrucciones = new int[400];
    static int[] registros = new int[32];
    static int[] instruccionIF = new int[4];
    static int[] instruccionID = new int[4];
    static int[] instruccionEX = new int[4];
    static int[] instruccionMEM = new int[4];
    static int[] instruccionWB = new int[4];
    static int pc = 0;
    static int[] tablaReg = new int[32]; //para que a ana no se le olvide que es para identificar conflictos
    static int[] banderaFin = new int[5];  //vector de banderas donde indican la finalización del programa para cada etapa
    static int resultadoEM = 0; // EX le pasa el resultado a Mem
    static int resultadoMem = 0; // es el resultado para lw y sw
    static int valMemoriaLW = 0; //
    static int resultadoMW = 0; // de Men a Wb 
    private static Semaphore[] sem = new Semaphore[]{new Semaphore(1), new Semaphore(1), new Semaphore(1), new Semaphore(1), new Semaphore(1)};
    private static Semaphore semGeneral = new Semaphore(0);
    
    
    public static void main(String[] args) {

        for (int i = 0; i < 200; i++) {             //Inicializa el vector de datos
            datos[i] = 0;
        }

        for (int i = 0; i < 400; i++) {             //Inicializa el vector de instrucciones
            instrucciones[i] = 0;
        }

        for (int i = 0; i < 32; i++) {              //Inicializa el vector de registros
            tablaReg[i] = 0;
        }

        for (int i = 0; i < 5; i++) {               //Inicializa el vector de banderas
            banderaFin[i] = 0;
        }

        //Hilos
        final Runnable instructionFetch = new Runnable() {
            public void run() {
                while (banderaFin[0] == 0) {

                    for (int i = 0; i < 4; i++) {
                        instruccionIF[i] = instrucciones[(pc * 4) + i];
                        //System.out.println(instruccionIF[i]);

                    }

                    try {
                        sem[0].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    for (int i = 0; i < 4; i++) {
                        instruccionID[i] = instruccionIF[i];
                    }

                    if (instruccionIF[0] == 63) {
                        banderaFin[0] = 1;
                    }

                    sem[0].release();
                    try {
                        semGeneral.acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };

        final Runnable instructionDecode = new Runnable() {
            public void run() {
                while (banderaFin[1] == 0) {

                    try {
                        sem[0].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    int opCode = instruccionID[0];
                    int op1 = instruccionID[1];
                    int op2 = instruccionID[2];
                    int op3 = instruccionID[3];

                    if (instruccionID[0] == 63) {
                        banderaFin[0] = 1;
                    }

                    for (int i = 0; i < 4; i++) {     //Imprime los valores del vector de instrucción
                        System.out.print(instruccionID[i]);
                    }

                    try {
                        sem[1].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    //switch que identifique el OP
                    switch (opCode) {
                        case 8: {                   //DADDI
                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0) {  //Si los registros op1 y op2 están libres
                                for (int i = 0; i < 4; i++) {
                                    instruccionEX[i] = instruccionID[i];
                                }
                            }
                        }
                        case 32: {                  //DADD

                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                                for (int i = 0; i < 4; i++) {
                                    instruccionEX[i] = instruccionID[i];
                                }
                            }
                        }
                        case 34: {                  //DSUB

                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                                for (int i = 0; i < 4; i++) {
                                    instruccionEX[i] = instruccionID[i];
                                }
                            }
                        }
                        case 12: {                  //DMUL

                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                                for (int i = 0; i < 4; i++) {
                                    instruccionEX[i] = instruccionID[i];
                                }
                            }
                        }
                        case 14: {                  //DDIV
                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                                for (int i = 0; i < 4; i++) {
                                    instruccionEX[i] = instruccionID[i];
                                }
                            }
                        }
                        case 35: {                  //LW
                            if (tablaReg[op2] == 0) {                                             //Si el registros op2 está libre
                                for (int i = 0; i < 4; i++) {
                                    instruccionEX[i] = instruccionID[i];
                                }
                            }
                        }
                        case 43: {                  //SW
                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0) {                       //Si los registros op1 y op2 están libres
                                for (int i = 0; i < 4; i++) {
                                    instruccionEX[i] = instruccionID[i];
                                }
                            }
                        }
                        case 4: {                  //BEQZ

                        }
                        case 5: {                  //BNEZ

                        }
                        case 3: {                  //JAL

                        }
                        case 2: {                  //JR

                        }
                        case 63: {                  //FIN
                            //Avisa que esta terminando
                        }
                    }

                    sem[0].release();
                    sem[1].release();
                    try {
                        semGeneral.acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }

        };

        final Runnable execute = new Runnable() {
            public void run() {
                while (banderaFin[2] == 0) {
                    try {
                        sem[1].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    int codigop = instruccionEX[0];
                    int op1 = instruccionEX[1];
                    int op2 = instruccionEX[2];
                    int op3 = instruccionEX[3];
                    int resultado = 0;

                    if (instruccionEX[0] == 63) {
                        banderaFin[0] = 1;
                    }

                    switch (codigop) {
                        case 8: {                   //DADDI
                            resultado = daddi(op1, op2, op3);
                        }
                        case 32: {                  //DADD
                            resultado = dadd(op1, op2, op3);
                        }
                        case 34: {                  //DSUB
                            resultado = dsub(op1, op2, op3);
                        }
                        case 12: {                  //DMUL
                            resultado = dmul(op1, op2, op3);
                        }
                        case 14: {                  //DDIV
                            resultado = ddiv(op1, op2, op3);
                        }
                        case 35: {                  //LW
                            resultado = lw(op1, op2, op3);
                        }
                        case 43: {                  //SW
                            resultado = sw(op1, op2, op3);
                        }
                        case 4: {                  //BEQZ
                            resultado = beqz(op1, op2, op3);
                        }
                        case 5: {                  //BNEZ
                            resultado = bnez(op1, op2, op3);
                        }
                        case 3: {                  //JAL
                            resultado = jal(op3);
                        }
                        case 2: {                  //JR
                            resultado = jr(op1);
                        }
                        case 63: {                  //FIN
                            //Avisa que esta terminando
                        }

                    }

                    try {
                        sem[2].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    for (int i = 0; i < 4; i++) {
                        instruccionMEM[i] = instruccionEX[i];
                    }

                    resultadoEM = resultado;

                    sem[1].release();
                    sem[2].release();
                    try {
                        semGeneral.acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };

        final Runnable memory = new Runnable() {
            public void run() {
                while (banderaFin[3] == 0) {

                    try {
                        sem[2].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    int op2 = instruccionMEM[0];
                    valMemoriaLW = resultadoEM;
                    if (instruccionMEM[0] == 63) {
                        banderaFin[0] = 1;
                    }

                    if (instruccionMEM[0] == 35 || instruccionMEM[0] == 43) {
                        resultadoMem = resultadoEM;
                        if (instruccionMEM[0] == 43) { // este puede escribir
                            datos[resultadoMem * 4] = op2; //se le guarda el valor del registro
                        } else {
                            valMemoriaLW = datos[resultadoMem * 4];
                        } //saca el valor de memoria y lo guarda en esta variable
                    }

                    try {
                        sem[3].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    for (int i = 0; i < 4; i++) {
                        instruccionWB[i] = instruccionMEM[i];
                    }
                    resultadoMW = valMemoriaLW;

                    sem[3].release();
                    sem[4].release();
                    try {
                        semGeneral.acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }
        };

        final Runnable writeBack = new Runnable() {
            public void run() {
                while (banderaFin[4] == 0) {

                    try {
                        sem[4].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    int codop = instruccionWB[0];
                    int op1 = instruccionWB[2];
                    int op2 = instruccionWB[2];
                    int op3 = instruccionWB[3];

                    if (instruccionWB[0] == 63) {
                        banderaFin[0] = 1;
                    }

                    if (codop == 8 || codop == 35) {
                        registros[op2] = resultadoMW;
                    } else {
                        registros[op3] = resultadoMW;// las operaciones aritmeticas de add, sub, mul y div
                    }
                    //Liberacion de los registros
                    if (codop == 8 || codop == 35) {
                        tablaReg[op1] = 0;
                        tablaReg[op2] = 0;
                    } else { // las operaciones aritmeticas de add, sub, mul y div
                        tablaReg[op1] = 0;
                        tablaReg[op2] = 0;
                        tablaReg[op3] = 0;
                    }
                    sem[4].release();
                    try {
                        semGeneral.acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
            }
        };

        Runnable principalThread = new Runnable() {
            public void run() {
                MIPS mips = new MIPS();
                mips.cargarInstrucciones();

                for (int i = 0; i < mips.instrucciones.length; i++) {
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
    static int daddi(int ry, int rx, int n) {
        int resultado = -1;
        return resultado;
    }

    static int dadd(int ry, int rz, int rx) {
        int resultado = -1;
        return resultado;
    }

    static int dsub(int ry, int rz, int rx) {
        int resultado = -1;
        return resultado;
    }

    static int dmul(int ry, int rz, int rx) {
        int resultado = -1;
        return resultado;
    }

    static int ddiv(int ry, int rz, int rx) {
        int resultado = -1;
        return resultado;
    }

    static int lw(int ry, int rx, int n) {
        int resultado = -1;
        return resultado;
    }

    static int sw(int ry, int rx, int n) {
        int resultado = -1;
        return resultado;
    }

    static int bnez(int rx, int label, int n) {
        int resultado = -1;
        return resultado;
    }

    static int beqz(int rx, int label, int n) {
        int resultado = -1;
        return resultado;
    }

    static int jal(int n) {
        int resultado = -1;
        return resultado;
    }

    static int jr(int rx) {
        int resultado = -1;
        return resultado;
    }

    void cargarInstrucciones() {
        try {
            BufferedReader bf = new BufferedReader(new FileReader("HILO-1.txt"));
            String linea = "";
            int i = 0;
            while ((linea = bf.readLine()) != null) {
                String[] parts = linea.split("\\s");
                for (String part : parts) {
                    instrucciones[i] = Integer.valueOf(part);
                    i++;
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
