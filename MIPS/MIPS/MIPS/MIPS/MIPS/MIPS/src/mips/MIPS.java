/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mips;

import java.lang.Thread;
import java.io.*;
import java.io.File;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.*;

public class MIPS {

    /* Estados: 1  = modificado
     *           2  = compartido
     *           -1 = invalido
     */
    static int clock = 1;
    static int[] datos = new int[832];
    static int[] instrucciones = new int[768];
    static int[] registros = new int[32];
    static int RL = -1;
    static int[][] cache = new int[6][8];
    static int[] instruccionIF = new int[4];
    static int[] instruccionID = new int[5];
    static int[] instruccionEX = new int[5];
    static int[] instruccionMEM = new int[5];
    static int[] instruccionWB = new int[5];
    static int pc = 0;
    static int[] tablaReg = new int[32];
    static int tablaRL = 0;
    static int[] banderaFin = new int[5];                     // indica la finalización del programa para cada etapa. 1 = FIN
    static int resultadoEM = 0;                              // EX le pasa el resultado a Mem
    static int resultadoMem = 0;                             // es el resultado para lw y sw
    static int valMemoriaLW = 0;
    static int resultadoMW = 0;                              // de Men a Wb 
    private static Semaphore[] sem = new Semaphore[]{new Semaphore(1), new Semaphore(1), new Semaphore(1), new Semaphore(1)};
    private static Semaphore semReg = new Semaphore(1);
    private static Semaphore semPC = new Semaphore(1);
    private static Semaphore semMataProc = new Semaphore(1);
    private static Semaphore semMataProc2 = new Semaphore(1);
    private static Semaphore semEsperaProc = new Semaphore(1);
    static CyclicBarrier barrier = new CyclicBarrier(6);

    static CyclicBarrier IFaID = new CyclicBarrier(2);
    static CyclicBarrier IDaEX = new CyclicBarrier(2);
    static CyclicBarrier EXaMEM = new CyclicBarrier(2);
    static CyclicBarrier MEMaWB = new CyclicBarrier(2);

    static int numHilos = 0;
    static int salida = 0;
    static int contadorQuantum = 0;
    static int quantum = 0;
    static int hiloEnEjecucion = 0;
    static int inicioInstHilos[];
    static int regProcesos[][];
    static int RLProcesos[];
    static int PCProcesos[];
    static int relojProcesos[];
    static int HilosCompletados = 0;
    static int colaDeEjecucion[];
    static int result = 0;

    public static void main(String[] args) {

        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 8; j++) {
                cache[i][j] = -2;
            }
        }

        for (int i = 0; i < datos.length; i++) {             //Inicializa el vector de datos
            datos[i] = 1;
        }

        for (int i = 0; i < instrucciones.length; i++) {             //Inicializa el vector de instrucciones
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
                while (HilosCompletados < numHilos) {
                    while (banderaFin[0] == 0) {

                        // System.out.println("PC: " + pc);
                        // Copia la instrucción de la "memoria" al vector de instrucción de IF
                        if (instruccionIF[0] != 64) {
                            for (int i = 0; i < 4; i++) {
                                instruccionIF[i] = instrucciones[(pc) + i];
                            }

                            pc += 4;
                        }
                        semPC.release();

                        /*
                         System.out.println("Instruccion en IF:\t");
                         for (int i = 0; i < 4; i++) {
                         System.out.print(instruccionIF[i] + "\t");
                         }
                         System.out.println("");
                         */
                        if ((instruccionIF[0] == 63) || (instruccionIF[0] == 64)) //Si la instrucción es FIN
                        {
                            banderaFin[0] = 1;
                        }

                        try {
                            sem[0].acquire();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        if (semMataProc.tryAcquire()) {
                            pc += -4;
                            if(instruccionIF[0] == 64)
                            {cambioEtapa(0);}
                            else
                            {cambioEtapa(-5);}                          
                        } else if (semMataProc2.tryAcquire()) {
                            if(instruccionIF[0] == 64)
                            {cambioEtapa(0);}
                            else
                            {cambioEtapa(-5);}  
                        } else if (semEsperaProc.tryAcquire()) {
                            pc += -4;
                            if ((instruccionIF[0] == 63) || (instruccionIF[0] == 64)) //Si la instrucción es FIN
                            {
                                banderaFin[0] = 0;
                            }
                        } else {
                            cambioEtapa(0);
                            System.out.println("pasé x aqui");
                        }

                        sem[0].release();
                        try {
                            barrier.await();
                            barrier.await();                // Este await es para que el hilo general pueda hacer una actualización entre esperas
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }

                    while (salida != 1) {
                        try {
                            barrier.await();                           
                            barrier.await();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }
            }
        };

        final Runnable instructionDecode = new Runnable() {
            public void run() {
                while (HilosCompletados < numHilos) {
                    while (banderaFin[1] == 0) {
                        int opCode = instruccionID[0];
                        int op1 = instruccionID[1];
                        int op2 = instruccionID[2];
                        int op3 = instruccionID[3];

                        /*
                         System.out.println("Instruccion en ID:\t");
                         for (int i = 0; i < 4; i++) {
                         System.out.print(instruccionID[i] + "\t");
                         }
                         System.out.println("");
                         */
                        try {
                            sem[1].acquire();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        try {
                            semReg.acquire();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        if ((instruccionID[0] == 63) || (instruccionID[0] == 64)) {
                            cambioEtapa(1);
                            banderaFin[1] = 1;
                        }

                        if (opCode == 8) {
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0) {  //Si los registros op1 y op2 están libres
                                instruccionID[4] = op2;
                                cambioEtapa(1);
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 35) {
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0) {	//Si el registro op2 está libre
                                instruccionID[4] = op2;
                                cambioEtapa(1);
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 32) {
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[instruccionID[3]] == 0) {  //Si los registros op1,op2, op3 están libres
                                instruccionID[4] = op3;
                                cambioEtapa(1);
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaReg[instruccionID[3]]++;
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 12) {
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[instruccionID[3]] == 0) {  //Si los registros op1,op2, op3 están libres
                                instruccionID[4] = op3;
                                cambioEtapa(1);
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaReg[instruccionID[3]]++;
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 14) {
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[instruccionID[3]] == 0) {  //Si los registros op1,op2, op3 están libres
                                instruccionID[4] = op3;
                                cambioEtapa(1);
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaReg[instruccionID[3]]++;
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 34) {
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[instruccionID[3]] == 0) {  //Si los registros op1,op2, op3 están libres
                                instruccionID[4] = op3;
                                cambioEtapa(1);
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaReg[instruccionID[3]]++;
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 43) {
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0) {  //Si los registros op1 y op2 están libres
                                instruccionID[4] = op2;
                                cambioEtapa(1);
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 4) {                           //BEQZ
                            if (tablaReg[instruccionID[1]] == 0) {
                                if (registros[instruccionID[1]] == 0) {
                                    try {
                                        semPC.acquire();
                                    } catch (InterruptedException ex) {
                                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    pc += (instruccionID[3] * 4);
                                    semPC.release();
                                    semMataProc.release();
                                    cambioEtapa(1);
                                } else {
                                    semMataProc.release();
                                    cambioEtapa(1);
                                }
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 5) {    //BNEZ
                            if (tablaReg[instruccionID[1]] == 0) {
                                System.out.println("segundo if ");
                                if (registros[instruccionID[1]] != 0) {
                                    System.out.println("tercer if");
                                    try {
                                        semPC.acquire();
                                    } catch (InterruptedException ex) {
                                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    pc += (instruccionID[3] * 4);
                                    semPC.release();
                                    semMataProc.release();
                                    cambioEtapa(1);
                                    System.out.println("pc: " + pc);
                                    System.out.println("pase por BNEZ ");
                                } else {
                                    System.out.println("pase por BNEZ ");
                                    System.out.println("pc: " + pc);
                                    semMataProc.release();
                                    cambioEtapa(1);
                                }
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 3) {                           //JAL
                            if (tablaReg[31] == 0) {
                                try {
                                    semPC.acquire();
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                registros[31] = pc;
                                pc += (instruccionID[1]);
                                semPC.release();
                                semMataProc.release();
                                cambioEtapa(1);
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 2) {                           //JR
                            if (tablaReg[instruccionID[1]] == 0) {
                                try {
                                    semPC.acquire();
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                pc = registros[instruccionID[1]];
                                semPC.release();
                                semMataProc.release();
                                cambioEtapa(1);
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 50) {                          //LL 
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaRL == 0) {  //Si los registros op1 y op2 están libres
                                instruccionID[4] = op2;
                                cambioEtapa(1);
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaRL++;
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 51) {                          //SC
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaRL == 0) {  //Si los registros op1 y op2 están libres
                                instruccionID[4] = op2;
                                cambioEtapa(1);
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaRL++;
                            } else {
                                cambioEtapa(-1);
                                semEsperaProc.release();
                            }
                        }

                        sem[1].release();
                        sem[0].release();
                        semReg.release();
                        try {
                            barrier.await();
                            barrier.await();                // Este await es para que el hilo general pueda hacer una actualización entre esperas
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }

                    while (salida != 1) {
                        try {
                            barrier.await();
                            barrier.await();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }
            }
        };

        final Runnable execute = new Runnable() {
            public void run() {
                while (HilosCompletados < numHilos) {
                    while (banderaFin[2] == 0) {
                        int opCode = instruccionEX[0];
                        int op1 = instruccionEX[1];
                        int op2 = instruccionEX[2];
                        int op3 = instruccionEX[3];
                        int regDestino = instruccionEX[4];
                        int resultado = 0;

                        /*
                         System.out.println("Instruccion en EX:\t");
                         for (int i = 0; i < 4; i++) {
                         System.out.print(instruccionEX[i] + "\t");
                         }
                         System.out.println("");
                         */
                        
                        if ((instruccionEX[0] == 63) || (instruccionEX[0] == 64)) {                  //FIN = 63
                            banderaFin[2] = 1;                                                       //CambioContexto = 64
                        }
                        
                        if (opCode == 8) {    //DADDI
                            resultado = daddi(op1, op2, op3);
                        }
                        if (opCode == 32) {   //DADD
                            resultado = dadd(op1, op2, op3);
                        }
                        if (opCode == 34) {   //DSUB
                            resultado = dsub(op1, op2, op3);
                        }
                        if (opCode == 12) {                  //DMUL
                            resultado = dmul(op1, op2, op3);
                        }
                        if (opCode == 14) {                  //DDIV
                            resultado = ddiv(op1, op2, op3);
                        }
                        if (opCode == 35) {                  //LW
                            resultado = lw(op1, op2, op3);
                        }
                        if (opCode == 43) {                  //SW
                            resultado = sw(op1, op2, op3);
                        }
                        if (opCode == 4) {                  //BEQZ
                        }
                        if (opCode == 5) {                  //BNEZ
                        }
                        if (opCode == 3) {                  //JAL
                        }
                        if (opCode == 2) {                  //JR
                        }
                        if (opCode == 50) {                 //LL
                            resultado = ll(op1, op3);
                        }
                        if (opCode == 51) {                  //SC
                            resultado = sc(op1, op3);
                        }

                        try {
                            sem[2].acquire();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        cambioEtapa(2);
                        resultadoEM = resultado;

                        sem[2].release();
                        sem[1].release();

                        try {
                            barrier.await();
                            barrier.await();                // Este await es para que el hilo general pueda hacer una actualización entre esperas
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }

                    while (salida != 1) {
                        try {
                            barrier.await();
                            barrier.await();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }

                }
            }
        };

        final Runnable memory = new Runnable() {
            public void run() {
                while (HilosCompletados < numHilos) {
                    while (banderaFin[3] == 0) {

                        int opCode = instruccionMEM[0];
                        int op1 = instruccionMEM[1];
                        int op2 = instruccionMEM[2];
                        int op3 = instruccionMEM[3];
                        int regDestino = instruccionMEM[4];
                        valMemoriaLW = resultadoEM;
                        resultadoMem = resultadoEM;
                        int bloque = (((resultadoMem) / 4) / 4) % 8;
                        /*
                         System.out.println("Instruccion en MEM:\t");
                         for (int i = 0; i < 4; i++) {
                         System.out.print(instruccionMEM[i] + "\t");
                         }
                         System.out.println("");
                         */
                        if ((instruccionMEM[0] == 63) || (instruccionMEM[0] == 64)) {
                            banderaFin[3] = 1;
                        }

                        if (opCode == 35) {         //load
                            if (hitDeEscritura(resultadoMem)) {
                                resultadoMem = cache[(resultadoMem % 16) / 4][bloque];
                            } else {
                                resolverFalloDeCache(resultadoMem);
                                resultadoMem = cache[(resultadoMem % 16) / 4][bloque];
                            }
                        }

                        if (opCode == 43) {         //store
                            try {
                                // Vieja forma de calcular posicion (resultadoMem % (((resultadoMem / 4) / 4) * 4)) / 4
                                if (hitDeEscritura(resultadoMem)) {
                                    cache[(resultadoMem % 16) / 4][bloque] = registros[regDestino];
                                    cache[5][bloque] = 1;               //pone el estado en modificado
                                } else {
                                    resolverFalloDeCache(resultadoMem);
                                    imprimirCache();
                                    cache[(resultadoMem % 16) / 4][bloque] = registros[regDestino];
                                    cache[5][bloque] = 1;               //pone el estado en modificado
                                }
                            } catch (java.lang.ArithmeticException exc) {
                                if (hitDeEscritura(resultadoMem)) {
                                    cache[0][0] = registros[regDestino];
                                    cache[0][5] = 1;
                                } else {
                                    resolverFalloDeCache(resultadoMem);
                                    cache[0][0] = registros[regDestino];
                                    cache[0][5] = 1;
                                }
                            }

                        }

                        if (opCode == 50) {                               //ll
                            try {
                                if (hitDeEscritura(resultadoMem)) {
                                 //  RL = resultadoMem;
                                 //   resultadoMem = cache[(resultadoMem % (((resultadoMem / 4) / 4) * 4)) / 4][((resultadoMem / 4) / 4) % 8];
                                } else {
                                    resolverFalloDeCache(resultadoMem);
                                 //   resultadoMem = cache[(resultadoMem % (((resultadoMem / 4) / 4) * 4)) / 4][((resultadoMem / 4) / 4) % 8];
                                 //   RL = resultadoMem;
                                }
                            } catch (java.lang.ArithmeticException exc) {
                                if (hitDeEscritura(resultadoMem)) {

                                //    resultadoMem = cache[0][0];
                                //    RL = resultadoMem;
                                } else {
                                    resolverFalloDeCache(resultadoMem);
                               //     resultadoMem = cache[0][0];
                               //     RL = resultadoMem;
                                }
                                
                            }

                        }

                        if (opCode == 51) {                                 //sc
                            try {
                                if (hitDeEscritura(resultadoMem)) {
                                    if (RL != -1) {                  //si es atómico
                                        if (resultadoMem == RL) {
                                            cache[(resultadoMem % 16) / 4][((resultadoMem / 4) / 4) % 8] = 1;
                                        }
                                    }
                                } else {
                                    resolverFalloDeCache(resultadoMem);
                                    if (RL != -1) {                  //si es atómico
                                        if (resultadoMem == RL) {
                                            cache[(resultadoMem % 16) / 4][((resultadoMem / 4) / 4) % 8] = 1;
                                        }
                                    }
                                }
                            } catch (java.lang.ArithmeticException exc) {

                                if (hitDeEscritura(resultadoMem)) {
                                    if (RL != -1) {                  //si es atómico
                                        if (resultadoMem == RL) {
                                            cache[0][0] = 1;
                                        }
                                    }
                                } else {
                                    resolverFalloDeCache(resultadoMem);
                                    if (RL != -1) {                  //si es atómico
                                        if (resultadoMem == RL) {
                                            cache[0][0] = 1;
                                        }
                                    }
                                }
                            }

                        }

                        try {
                            sem[3].acquire();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        cambioEtapa(3);
                        resultadoMW = resultadoMem;

                        sem[3].release();
                        sem[2].release();
                        try {
                            barrier.await();
                            barrier.await();                // Este await es para que el hilo general pueda hacer una actualización entre esperas
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }

                    while (salida != 1) {
                        try {
                            barrier.await();
                            barrier.await();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        };

        final Runnable writeBack = new Runnable() {
            public void run() {
                while (HilosCompletados < numHilos) {
                    while (banderaFin[4] == 0) {
                        int opCode = instruccionWB[0];
                        int op1 = instruccionWB[1];
                        int op2 = instruccionWB[2];
                        int op3 = instruccionWB[3];
                        int regDestino = instruccionWB[4];

                        /*
                         System.out.println("Instruccion en WB:");
                         for (int i = 0; i < 4; i++) {
                         System.out.print(instruccionWB[i] + "\t");
                         }
                         System.out.println("");
                         */
                        if ((instruccionWB[0] == 63) || (instruccionWB[0] == 64)) {
                            if (instruccionWB[0] == 64) {
                                banderaFin[4] = 2;

                            } else {
                                banderaFin[4] = 1;
                                colaDeEjecucion[hiloEnEjecucion] = 1;
                                HilosCompletados += 1;
                            }
                            System.out.println("hola llegue al final :)");
                            System.out.println("pc : " + pc);
                        }

                        if (opCode == 8) {
                            registros[regDestino] = resultadoMW;
                        }
                        if (opCode == 35) {
                            registros[regDestino] = resultadoMW;
                        }
                        if (opCode == 32) {
                            registros[regDestino] = resultadoMW;
                        }
                        if (opCode == 12) {
                            registros[regDestino] = resultadoMW;
                        }
                        if (opCode == 14) {
                            registros[regDestino] = resultadoMW;
                        }
                        if (opCode == 34) {
                            registros[regDestino] = resultadoMW;
                        }
                        if (opCode == 43) {

                        }
                        if (opCode == 50) {         //ll
                            try{
                                //Vieja forma de calcular posicion (resultadoMW % (((resultadoMW / 4) / 4) * 4)) / 4
                                RL = resultadoMW;
                                registros[regDestino] = cache[(resultadoMW % 16) / 4][(((resultadoMW) / 4) / 4) % 8];
                            }
                            catch(java.lang.ArithmeticException exc){
                                RL = resultadoMW;
                                registros[regDestino] = cache[0][0];
                            }

                        }
                        if (opCode == 51) {         //sc
                            if (RL != -1) {                  //si es atómico
                                registros[regDestino] = 1;
                            } else {
                                registros[regDestino] = 0;
                            }
                        }

                        //Liberacion de los registros
                        if (opCode == 8 && (tablaReg[op1] > 0) && (tablaReg[op2] > 0)) {
                            tablaReg[op1]--;
                            tablaReg[op2]--;
                        }
                        if (opCode == 35 && (tablaReg[op1] > 0) && (tablaReg[op2] > 0)) {
                            tablaReg[op1]--;
                            tablaReg[op2]--;
                        }
                        if (opCode == 32 && (tablaReg[op1] > 0) && (tablaReg[op2] > 0) && (tablaReg[op3] > 0)) {
                            tablaReg[op1]--;
                            tablaReg[op2]--;
                            tablaReg[op3]--;
                        }
                        if (opCode == 12 && (tablaReg[op1] > 0) && (tablaReg[op2] > 0) && (tablaReg[op3] > 0)) {
                            tablaReg[op1]--;
                            tablaReg[op2]--;
                            tablaReg[op3]--;
                        }
                        if (opCode == 14 && (tablaReg[op1] > 0) && (tablaReg[op2] > 0) && (tablaReg[op3] > 0)) {
                            tablaReg[op1]--;
                            tablaReg[op2]--;
                            tablaReg[op3]--;
                        }
                        if (opCode == 34 && (tablaReg[op1] > 0) && (tablaReg[op2] > 0) && (tablaReg[op3] > 0)) {
                            tablaReg[op1]--;
                            tablaReg[op2]--;
                            tablaReg[op3]--;
                        }
                        if (opCode == 43 && (tablaReg[op1] > 0) && (tablaReg[op2] > 0)) {
                            tablaReg[op1]--;
                            tablaReg[op2]--;
                        }
                        if (opCode == 50 && (tablaReg[op1] > 0) && (tablaReg[op2] > 0) && (tablaRL > 0)) {
                            tablaReg[op1]--;
                            tablaReg[op2]--;
                            tablaRL--;
                        }
                        if (opCode == 51 && (tablaReg[op1] > 0) && (tablaReg[op2] > 0)  && (tablaRL > 0)) {
                            tablaReg[op1]--;
                            tablaReg[op2]--;
                            tablaRL--;
                        }

                        sem[3].release();
                        semReg.release();

                        try {
                            barrier.await();
                            barrier.await();                // Este await es para que el hilo general pueda hacer una actualización entre esperas
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        };

        Runnable mainThread;
        mainThread = new Runnable() {
            public void run() {

                cargarInstrucciones();
                contadorQuantum = quantum;

                semEsperaProc.drainPermits();
                semMataProc.drainPermits();
                semMataProc2.drainPermits();
                semPC.drainPermits();
                sem[0].drainPermits();
                sem[1].drainPermits();
                sem[2].drainPermits();
                sem[3].drainPermits();

                // inicia los hilos
                new Thread(instructionFetch).start();
                new Thread(instructionDecode).start();
                new Thread(execute).start();
                new Thread(memory).start();
                new Thread(writeBack).start();

                while (/*(banderaFin[0] == 0 || banderaFin[1] == 0 || banderaFin[2] == 0 || banderaFin[3] == 0 || banderaFin[4] == 0)*/HilosCompletados < numHilos) {
                    try {
                        barrier.await();

                        semEsperaProc.drainPermits();
                        semMataProc.drainPermits();
                        semMataProc2.drainPermits();
                        semPC.drainPermits();
                        sem[0].drainPermits();
                        sem[1].drainPermits();
                        sem[2].drainPermits();
                        sem[3].drainPermits();

                        try {
                            semReg.acquire();

                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        contadorQuantum--;
                        clock++;

                        if (contadorQuantum == 0) {
                            for (int g = 0; g < 4; g++) {
                                instruccionIF[g] = 64;
                            }
                        }
                        
                        if(instruccionWB[0] == 0){
                            salida = 0;                   
                        }

                        if (banderaFin[4] == 1 || banderaFin[4] == 2) {
                            cacheAMemoria();
                            cambioDeContexto();
                            contadorQuantum = quantum;
                            if (hiloEnEjecucion != -1) {
                                for (int i = 0; i < 5; i++) {
                                    banderaFin[i] = 0;
                                }
                            }
                            salida = 1;
                        }
                        barrier.await();

                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                //imprimirVecInstrucciones();
                imprimirRegistros();
                imprimirCache();
                cacheAMemoria();
                imprimirVecDatos();

                System.out.println("El valor del reloj es: " + clock);
                System.exit(clock);
            }
        };

        new Thread(mainThread).start();
    }

    //Operaciones 
    static int daddi(int ry, int rx, int n) {
        int resultado = registros[ry] + n;
        return resultado;
    }

    static int dadd(int ry, int rz, int rx) {
        int resultado = registros[ry] + registros[rz];
        return resultado;
    }

    static int dsub(int ry, int rz, int rx) {
        int resultado = registros[ry] - registros[rz];
        return resultado;
    }

    static int dmul(int ry, int rz, int rx) {
        int resultado = registros[ry] * registros[rz];
        return resultado;
    }

    static int ddiv(int ry, int rz, int rx) {
        int resultado = registros[ry] / registros[rz];
        return resultado;
    }

    static int lw(int ry, int rx, int n) {
        int resultado = n + registros[ry] - 768;
        return resultado;
    }

    static int sw(int ry, int rx, int n) {
        int resultado = n + registros[ry] - 768;
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
        int resultado = n;
        return resultado;
    }

    static int jr(int rx) {
        int resultado = rx;
        return resultado;
    }

    static int ll(int rx, int n) {
        int resultado = n + registros[rx] - 768;
        return resultado;
    }

    static int sc(int rx, int n) {
        int resultado = n + registros[rx] - 768;
        return resultado;
    }

    static void creaEstructuras(int cant) {
        colaDeEjecucion = new int[cant];
        inicioInstHilos = new int[cant];
        regProcesos = new int[32][cant];
        RLProcesos = new int[cant];
        PCProcesos = new int[cant];
        relojProcesos = new int[cant];

        for (int i = 0; i < cant; i++) {
            RLProcesos[i] = -1;
            PCProcesos[i] = 0;
            relojProcesos[i] = 0;
            inicioInstHilos[i] = 0;
        }

        for (int i = 0; i < cant; i++) {
            for (int j = 0; j < 32; j++) {
                regProcesos[j][i] = 0;
            }
        }

    }

    static void cargarInstrucciones() {
        try {
            JFileChooser loadEmp = new JFileChooser();//new dialog
            File[] seleccionados;//needed*
            BufferedReader bf;//needed*
            int i = 0;
            String linea = "";

            File directorio = new File(System.getProperty("user.dir"));
            loadEmp.setCurrentDirectory(directorio);
            loadEmp.setMultiSelectionEnabled(true);
            loadEmp.showOpenDialog(null);
            seleccionados = loadEmp.getSelectedFiles();

            /*
             seleccionados = loadEmp.getSelectedFiles();
             if (1 == loadEmp.CANCEL_OPTION)
             { System.exit(0);}
             */
            numHilos = seleccionados.length;
            int numHilo = 0;
            creaEstructuras(seleccionados.length);

            for (File seleccionado : seleccionados) {
                bf = new BufferedReader(new FileReader(seleccionado));
                System.out.println(seleccionado.toString());

                inicioInstHilos[numHilo] = i;
                while ((linea = bf.readLine()) != null) {
                    String[] parts = linea.split("\\s");
                    for (String part : parts) {
                        instrucciones[i] = Integer.valueOf(part);
                        i++;
                    }
                    System.out.println(linea);
                    //close stream, files stops loading
                }
                numHilo += 1;
                bf.close();
            }
        } catch (IOException ex) {
        } //catches nullpointer exception, file not found
        catch (NullPointerException ex) {
        }

        for (int j = 0; j < inicioInstHilos.length; j++) {
            PCProcesos[j] = inicioInstHilos[j];
        }

        pedirQuantum();
    }

    static void imprimirVecInstrucciones() {
        for (int i = 0; i < instrucciones.length; i++) {
            if (i % 4 == 0) //Si es múltiplo de 4       
            {
                System.out.println("");   //cambio de linea
            }
            System.out.print(Integer.toString(instrucciones[i]) + "\t");
        }
        System.out.println("");
    }

    static void imprimirRegistros() {
        System.out.println("REGISTROS");
        for (int i = 0; i < registros.length; i++) {
            System.out.println("R" + i + ": " + registros[i] + " ");
        }
        System.out.println("");
    }

    static void imprimirTabRegistros() {
        System.out.println("REGISTROS");
        for (int i = 0; i < tablaReg.length; i++) {
            System.out.println("R" + i + ": " + tablaReg[i] + " ");
        }
        System.out.println("");
    }

    static void imprimirVecDatos() {
        System.out.println("MEMORIA");
        for (int i = 0; i < datos.length; i++) {
            if (i % 10 == 0) //Si es múltiplo de 4       
            {
                System.out.println("");   //cambio de linea
            }
            System.out.print(datos[i] + "\t");
        }
        System.out.println("\n");
    }

    static void cambioEtapa(int x) {
        if (x == -5) {                                  //Este es el caso en que se mata la instrucción en IF y se pasa una operación vacía a ID
            for (int i = 0; i < 5; i++) {
                instruccionID[i] = 0;
            }
        } else if (x == -1) {
            for (int i = 0; i < 5; i++) {
                instruccionEX[i] = 0;
            }
        } else if (x == 0) {
            for (int i = 0; i < 4; i++) {
                instruccionID[i] = instruccionIF[i];
            }
        } else if (x == 1) {
            for (int i = 0; i < 5; i++) {
                instruccionEX[i] = instruccionID[i];
            }
        } else if (x == 2) {
            for (int i = 0; i < 5; i++) {
                instruccionMEM[i] = instruccionEX[i];
            }
        } else if (x == 3) {
            for (int i = 0; i < 5; i++) {
                instruccionWB[i] = instruccionMEM[i];
            }
        }
    }

    static void imprimirCache() {
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 8; j++) {
                System.out.print(cache[i][j] + "\t");
            }
            System.out.print("\n");
        }

    }

    static boolean hitDeEscritura(int dir) {
        boolean x = false;

        int bloque = (((dir) / 4) / 4) % 8;
        if (cache[4][bloque] == ((dir / 4) / 4)) { // si el bloque está en caché

            if (cache[5][bloque] == 1) { //modificado
                x = true;
            }

            if (cache[5][bloque] == 2) { //compartido
                x = true;
            }

        }

        return x;
    }

    static void resolverFalloDeCache(int dir) {
        //System.out.println("Antes del Fallo \n");

        System.out.println("\n");
        int bloque = (((dir) / 4) / 4) % 8;

        if ((cache[5][bloque] == 1)) { //modificado

            for (int i = 0; i < 4; i++) {
                datos[(cache[4][bloque] * 4) + i] = cache[i][bloque];
            }

        }

        for (int i = 0; i < 4; i++) {               //Sube los datos de memoria a cache
            cache[i][bloque] = datos[(((dir / 4) / 4) * 4) + (i)]; // :)
        }
        cache[4][bloque] = ((dir) / 4) / 4;                   //cambia la etiqueta
        cache[5][bloque] = 2;                       // estado = compartido
        //System.out.println("Despues del Fallo \n");
        //imprimirCache();
    }

    static void cacheAMemoria() {
        for (int p = 0; p < 8; p++) {
            for (int i = 0; i < 4; i++) {
                if (cache[5][p] == 1) {                       //si está modificado
                    datos[(cache[4][p] * 4) + i] = cache[i][p];
                }
            }
        }

    }

    static void pedirQuantum() {
        try {
            JFrame frame = new JFrame("¥*¥");
            String name = JOptionPane.showInputDialog(frame, "Cual es el quantum? Por favor ingrese un numero sino no saldra de aqui.");
            quantum = Integer.parseInt(name);
        } catch (Exception e) {
            pedirQuantum();
        }

    }

    static void cambioDeContexto() {
        boolean si = false;

        for (int i = 0; i < 32; i++) {
            regProcesos[i][hiloEnEjecucion] = registros[i];
        }
        PCProcesos[hiloEnEjecucion] = pc;
        RLProcesos[hiloEnEjecucion] = -1;

        for (int n = 1; n <= numHilos; n++) {
            if (!si && colaDeEjecucion[(hiloEnEjecucion + n) % numHilos] == 0) {
                hiloEnEjecucion = (hiloEnEjecucion + n) % numHilos;
                si = true;
            }
        }

        if (!si) {
            hiloEnEjecucion = -1;
        }

        if (si) {
            for (int i = 0; i < 32; i++) {
                registros[i] = regProcesos[i][hiloEnEjecucion];
            }
            pc = PCProcesos[hiloEnEjecucion];
            RL = RLProcesos[hiloEnEjecucion];

        }
        for (int y = 0; y < 4; y++) {
            instruccionIF[y] = 0;
            instruccionID[y] = 0;
            instruccionEX[y] = 0;
            instruccionMEM[y] = 0;
            instruccionWB[y] = 0;
        }
        instruccionID[4] = 0;
        instruccionEX[4] = 0;
        instruccionMEM[4] = 0;
        instruccionWB[4] = 0;

    }

}
