/** 
 * Universidad de Costa Rica
 * Proyecto de Arquitectura de Computadoras, II Semestre 2014
 * Integrantes:
 *  Kay Sander Carazo             B16162
 *  Jose Slon Baltodano           B06066
 *  Ana Laura Berdasco Romero     B10942
 *  Jennifer Ledezma              B13616
 **/

package mips;

import java.lang.Thread;
import java.io.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.FileWriter;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.*;

public class MIPS {

    /**
     * Estados para el manejo de la caché: 
     *             1  = modificado
     *             2  = compartido
     *            -1 = invalido
     **/
    
    static int clock = 1;                                                       //Reloj del pipeline
    static int[] datos = new int[832];                                          //Contiene los valores que se almacenan en memoria
    static int[] instrucciones = new int[768];                                  //Vector de instrucciones
    static int[] registros = new int[32];                                       //Vector de registros
    static int RL = -1;                                                         //Registro para verificar atomicidad entre LL y SC
    static int[][] cache = new int[6][8];                                       //Estructura para el manejo de la cache
    static int[] instruccionIF = new int[4];                                    //Estructura para el manejo de la instruccion que llega a If
    static int[] instruccionID = new int[5];                                    //Estructura para el manejo de la instruccion que llega a ID
    static int[] instruccionEX = new int[5];                                    //Estructura para el manejo de la instruccion que llega a EX
    static int[] instruccionMEM = new int[5];                                   //Estructura para el manejo de la instruccion que llega a MEM
    static int[] instruccionWB = new int[5];                                    //Estructura para el manejo de la instruccion que llega a WB
    static int pc = 0;                                                          // Tiempo requerido para avanzar una instruccion por el pipeline
    static int[] tablaReg = new int[32];                                        //Utilizada para saber si un regstro ya fue utilizado por una instruccion
    static int tablaRL = 0;
    static int[] banderaFin = new int[5];                                       // indica la finalización del programa para cada etapa. 1 = FIN
    static int resultadoEM = 0;                                                 // EX le pasa el resultado a Mem
    static int resultadoMem = 0;                                                // Es el resultado para lw y sw
    static int valMemoriaLW = 0;
    static int resultadoMW = 0;                                                 // de Mem a Wb 
    private static Semaphore[] sem = new Semaphore[]{ new Semaphore(1),new Semaphore(1), new Semaphore(1), new Semaphore(1)}; //semaforos para etapas intermedias
    private static Semaphore semReg = new Semaphore(1);                         //Semaforo para el manejo de los registros
    private static Semaphore semPC = new Semaphore(1);                          //Semaforo para manejar el paso de intrucciones por el pipeline
    private static Semaphore semMataProc = new Semaphore(1);                    //Semaforos utilizados para matar procesos
    private static Semaphore semMataProc2 = new Semaphore(1);                   //Se utilizan dos para lograr la sincronizacion entre id y if
    private static Semaphore semEsperaProc = new Semaphore(1);
    static CyclicBarrier barrier = new CyclicBarrier(6);

    static CyclicBarrier IFaID = new CyclicBarrier(2);                          //Maneja fase intermedia entre el Fetch y Decodificacion
    static CyclicBarrier IDaEX = new CyclicBarrier(2);                          //Maneja fase intermedia entre el Decodificacion y Execute
    static CyclicBarrier EXaMEM = new CyclicBarrier(2);                         //Maneja fase intermedia entre el  Execute y Memory
    static CyclicBarrier MEMaWB = new CyclicBarrier(2);                         //Maneja fase intermedia entre el Memory y la Write Back

    static int numHilos = 0;                                                    //Me indica cuantos hilos se deben de ejecutar
    static int salida = 0;                                                      //Controla las etapas desde IF hasta MEM
    static int contadorQuantum = 0;                                             //lleva el control del quantum para que se cumpla el ingresado por el usuario
    static int quantum = 0;                                                     //valor del quantum ingresado por el usuario
    static int hiloEnEjecucion = 0;                                             //indica cual hilo se esta ejecutando en este momento
    static int inicioInstHilos[];                                               //vector para indicar donde inicia cada hilo que fue cragado
    static int regProcesos[][]; 
    static int RLProcesos[];
    static int PCProcesos[];
    static int relojProcesos[];                                                 //Maneja el reloj de los procesos para las estructuras de control
    static int HilosCompletados = 0;                                            //Me indica cuantos hilos se han ejecutado.
    static int colaDeEjecucion[];                                               //Estructura que me permite llevar un control de los hilos que se han ejecutado y de los que aun les falta instrucciones
    static int result = 0;	
    static int relojInicial = 0;                                                // Es para manejar por separado y sumarle cuanto tarda cada hilo en ejecutarse.

    public static void main(String[] args) {

        for (int i = 0; i < 6; i++) {                                           //Inicializa la cache
            for (int j = 0; j < 8; j++) {
                cache[i][j] = -2;
            }
        }

        for (int i = 0; i < datos.length; i++) {                                //Inicializa el vector de datos
            datos[i] = 1;
        }

        for (int i = 0; i < instrucciones.length; i++) {                        //Inicializa el vector de instrucciones
            instrucciones[i] = 0;
        }

        for (int i = 0; i < 32; i++) {                                          //Inicializa el vector de registros
            tablaReg[i] = 0;
        }

        for (int i = 0; i < 5; i++) {                                           //Inicializa el vector de banderas
            banderaFin[i] = 0;
        }

        /**********************************************************************
         *                             HILOS
         **********************************************************************/
        
        /**
         * Hilo IF del pipeline: Se encarga de buscar al instruccion actual que 
         * debe de ser ejecutada, y actualiza el PC (para extraer la siguiente 
         * instrucción)
         */
        final Runnable instructionFetch = new Runnable() {                      //Hilo IF
            public void run() {
                while (HilosCompletados < numHilos) {
                    while (banderaFin[0] == 0) {                                // mientras los hilos no hayan terminado de ejecutarse 1=> se termino de ejecutar
                        
                        // Copia la instrucción de la "memoria" al vector de instrucción de IF
                        if (instruccionIF[0] != 64) {                           // Si puedo seguir ejecutando el hilo
                            for (int i = 0; i < 4; i++) {
                                instruccionIF[i] = instrucciones[(pc) + i];
                            }

                            pc += 4;                                            // Actualiza el PC para la siguiente intrucción (secuencial) le suma 4 por ser de 4 bytes la instruccion
                        }
                        semPC.release();                                        // se le vuelve a dar permisos al hilo

                        /*
                         System.out.println("Instruccion en IF:\t");
                         for (int i = 0; i < 4; i++) {
                         System.out.print(instruccionIF[i] + "\t");
                         }
                         System.out.println("");
                         */
                        
                        //La instruccion 64 trabaja como la instruccion fin pero se usa para el 
                        //cambio de contexto, esta instruccion indicaria que se ha finalizado el
                        //quantum para este hilo
                        if ((instruccionIF[0] == 63) || (instruccionIF[0] == 64))//Si la instrucción es FIN
                        {
                            banderaFin[0] = 1;                                   //Se termina de ejecutar el hilo
                        }

                        try {
                            sem[0].acquire();                                    //bloquea hasta que el recurso este disponible
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        if (semMataProc.tryAcquire()) {
                            pc += -4;
                            if(instruccionIF[0] == 64)
                            {cambioEtapa(0);}                                   //caso en que se pasa la intruccion de IF a ID
                            else
                            {cambioEtapa(-5);}                                  //se mata la instrucción en IF y se pasa una operación vacía a ID                         
                        } else if (semMataProc2.tryAcquire()) {
                            if(instruccionIF[0] == 64)
                            {cambioEtapa(0);}                                   //caso en que se pasa la intruccion de IF a ID
                            else
                            {cambioEtapa(-5);}                                  //se mata la instrucción en IF y se pasa una operación vacía a ID                               
                        } else if (semEsperaProc.tryAcquire()) {
                            pc += -4;
                            if ((instruccionIF[0] == 63) || (instruccionIF[0] == 64)) //Si la instrucción es FIN
                            {
                                banderaFin[0] = 0;
                            }
                        } else {
                            cambioEtapa(0);                                     //caso en que se pasa la intruccion de IF a ID
                        }

                        sem[0].release();
                        try {
                            barrier.await();
                            barrier.await();                                    // Este await es para que el hilo general pueda hacer una actualización entre esperas
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                    // Para poder coordinar correctamente que todas las etapas terminen en orden,
                    // esto para que el hilo principal tenga el control de que se esta ejecutando
                    while (salida != 1) {                                       
                        try {                                                   // Se ejecuta hasta que el wb salga
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
        
        /**
         * Hilo ID del pipeline: hace la funcion de decodificar la instruccion y 
         * leer los registros correspondientes (opCode, op1, op2, op3)
         */
        final Runnable instructionDecode = new Runnable() {
            public void run() {
                while (HilosCompletados < numHilos) {
                    while (banderaFin[1] == 0) {
                        int opCode = instruccionID[0];                          //Codigo de operacion (byte 1 de la instruccion)
                        int op1 = instruccionID[1];                             //Byte 2 de la instruccion
                        int op2 = instruccionID[2];                             //Byte 3 de la instruccion
                        int op3 = instruccionID[3];                             //Byte 4 de la instruccion

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
                        //Si el hilo a correr termino o hay un cambio de contexto
                        if ((instruccionID[0] == 63) || (instruccionID[0] == 64)) {
                            cambioEtapa(1);                                     //el caso en que se pasa la intruccion de ID a EX
                            banderaFin[1] = 1;
                        }
                        
                        //Primero se hace una verificación de los registros que las instrucciones 
                        //van a utilizar para saber si los registros que va utilizar se encuentran disponibles.                       
                        if (opCode == 8) {
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0) {  //Si los registros op1 y op2 están libres
                                instruccionID[4] = op2;
                                cambioEtapa(1);                                 //el caso en que se pasa la intruccion de ID a EX
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 35) {
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0) {	//Si el registro op2 está libre
                                instruccionID[4] = op2;
                                cambioEtapa(1);                                 //el caso en que se pasa la intruccion de ID a EX
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 32) {
                            //Si los registros op1,op2, op3 están libres
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[instruccionID[3]] == 0) {  
                                instruccionID[4] = op3;
                                cambioEtapa(1);                                 //el caso en que se pasa la intruccion de ID a EX
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaReg[instruccionID[3]]++;
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 12) {
                            //Si los registros op1,op2, op3 están libres
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[instruccionID[3]] == 0) {  
                                instruccionID[4] = op3;
                                cambioEtapa(1);                                 //el caso en que se pasa la intruccion de ID a EX
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaReg[instruccionID[3]]++;
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 14) {
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[instruccionID[3]] == 0) {  //Si los registros op1,op2, op3 están libres
                                instruccionID[4] = op3;
                                cambioEtapa(1);                                 //el caso en que se pasa la intruccion de ID a EX
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaReg[instruccionID[3]]++;
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 34) {
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[instruccionID[3]] == 0) {  //Si los registros op1,op2, op3 están libres
                                instruccionID[4] = op3;
                                cambioEtapa(1);                                 //el caso en que se pasa la intruccion de ID a EX
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaReg[instruccionID[3]]++;
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 43) {
                            //Si los registros op1 y op2 están libres
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0) {  
                                instruccionID[4] = op2;
                                cambioEtapa(1);                                 //el caso en que se pasa la intruccion de ID a EX
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 4) {                                      //BEQZ
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
                                    cambioEtapa(1);                             //el caso en que se pasa la intruccion de ID a EX
                                } else {
                                    semMataProc.release();
                                    cambioEtapa(1);                             //el caso en que se pasa la intruccion de ID a EX
                                }
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 5) {    //BNEZ
                            if (tablaReg[instruccionID[1]] == 0) {
                                if (registros[instruccionID[1]] != 0) {
                                    try {
                                        semPC.acquire();
                                    } catch (InterruptedException ex) {
                                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                                    }
                                    pc += (instruccionID[3] * 4);
                                    semPC.release();
                                    semMataProc.release();
                                    cambioEtapa(1);                             //el caso en que se pasa la intruccion de ID a EX                                   
                                } else {
                                    semMataProc.release();
                                    cambioEtapa(1);                             //el caso en que se pasa la intruccion de ID a EX
                                }
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 3) {                                      //JAL
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
                                cambioEtapa(1);                                 //el caso en que se pasa la intruccion de ID a EX
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 2) {                                      //JR
                            if (tablaReg[instruccionID[1]] == 0) {
                                try {
                                    semPC.acquire();
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                                }
                                pc = registros[instruccionID[1]];
                                semPC.release();
                                semMataProc.release();
                                cambioEtapa(1);                                 //el caso en que se pasa la intruccion de ID a EX
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 50) {                                     //LL 
                            //Si los registros op1 y op2 están libres
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaRL == 0) {  
                                instruccionID[4] = op2;
                                cambioEtapa(1);                                 //el caso en que se pasa la intruccion de ID a EX
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaRL++;
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }
                        if (opCode == 51) {                                     //SC
                            //Si los registros op1 y op2 están libres
                            if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaRL == 0) {  
                                instruccionID[4] = op2;
                                cambioEtapa(1);                                 //el caso en que se pasa la intruccion de ID a EX
                                tablaReg[instruccionID[1]]++;
                                tablaReg[instruccionID[2]]++;
                                tablaRL++;
                            } else {
                                cambioEtapa(-1);                                //se mata la instrucción en ID y se pasa una operación vacía a EX
                                semEsperaProc.release();
                            }
                        }

                        sem[1].release();
                        sem[0].release();
                        semReg.release();
                        try {
                            barrier.await();
                            barrier.await();                                    // Este await es para que el hilo general pueda hacer una actualización entre esperas
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                    // Para poder coordinar correctamente que todas las etapas terminen en orden,
                    // esto para que el hilo principal tenga el control de que se esta ejecutando
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
        
        /**
         * Hilo EX del pipeline: hace que el ALU opere sobre los operadores (Op1, Op2)
         * Se puede comunicar con MEM en el mismo ciclo de reloj
         */
        final Runnable execute = new Runnable() {
            public void run() {
                //Mientras hayan hilos que completar/ejecutar
                while (HilosCompletados < numHilos) {
                    while (banderaFin[2] == 0) {
                        //Cada operando extrae de EX los bytes de instruccion
                        int opCode = instruccionEX[0];
                        int op1 = instruccionEX[1];
                        int op2 = instruccionEX[2];
                        int op3 = instruccionEX[3];
                        int regDestino = instruccionEX[4];                      //Registro a almacenar
                        int resultado = 0;
                        
                        if ((instruccionEX[0] == 63) || (instruccionEX[0] == 64)) {  //FIN = 63
                            banderaFin[2] = 1;                                        //CambioContexto = 64
                        }
                        
                        //Se determina el resultadoo de la operacion segun el codigo presentado*/
                        if (opCode == 8) {                   //DADDI
                            resultado = daddi(op1, op2, op3);
                        }
                        if (opCode == 32) {                  //DADD
                            resultado = dadd(op1, op2, op3);
                        }
                        if (opCode == 34) {                  //DSUB
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
                        if (opCode == 4) {                   //BEQZ
                        }
                        if (opCode == 5) {                   //BNEZ
                        }
                        if (opCode == 3) {                   //JAL
                        }
                        if (opCode == 2) {                   //JR
                        }
                        if (opCode == 50) {                  //LL
                            resultado = ll(op1, op3);
                        }
                        if (opCode == 51) {                   //SC
                            resultado = sc(op1, op3);
                        }

                        try {
                            sem[2].acquire();
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        cambioEtapa(2);                                         //Es el caso en que se pasa la intruccion de EX a MEM
                        resultadoEM = resultado;

                        sem[2].release();
                        sem[1].release();

                        try {
                            barrier.await();
                            barrier.await();                                    // Este await es para que el hilo general pueda hacer una actualización entre esperas
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                    
                   // Para poder coordinar correctamente que todas las etapas terminen en orden,
                   // esto para que el hilo principal tenga el control de que se esta ejecutando
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
        
        /**
         * Hilo MEM del pipeline: Permite el acceso a memoria tomando las 
         * consideraciones de si la instruccion es un Load, Store o Load Link,
         * Store Conditional 
         */
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
                        int bloque = (((resultadoMem) / 4) / 4) % 8;            //Calcula el bloque de memoria 

                        if ((instruccionMEM[0] == 63) || (instruccionMEM[0] == 64)) {
                            banderaFin[3] = 1;
                        }
                        
                        //Mem lee la dir calculada en el ciclo anterior (EX)
                        if (opCode == 35) {                                     //LOAD
                            if (hitDeEscritura(resultadoMem)) {                        // Revisa si el bloque que necesito se encuentra en cache
                                resultadoMem = cache[(resultadoMem % 16) / 4][bloque];
                            } else {                                            
                                resolverFalloDeCache(resultadoMem);                    //Alguien más modifico el bloque que necesito
                                resultadoMem = cache[(resultadoMem % 16) / 4][bloque]; //Copia en memoria lo que hay en la caché
                            }
                        }
                        
                        // Hace lo mismo del load, puede escribir sin tomar en cuenta 
                        // que otra instruccion este usando el registro leído
                        if (opCode == 43) {                                     //STORE
                            try {
                                // Vieja forma de calcular posicion (resultadoMem % (((resultadoMem / 4) / 4) * 4)) / 4
                                if (hitDeEscritura(resultadoMem)) {
                                    cache[(resultadoMem % 16) / 4][bloque] = registros[regDestino];
                                    cache[5][bloque] = 1;                       //pone el estado en modificado
                                } else {
                                    resolverFalloDeCache(resultadoMem);
                                    cache[(resultadoMem % 16) / 4][bloque] = registros[regDestino];
                                    cache[5][bloque] = 1;                       //pone el estado en modificado
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
                        //
                        if (opCode == 50) {                                     //LL
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

                        if (opCode == 51) {                                     //SC
                            try {
                                if (hitDeEscritura(resultadoMem)) {
                                    if (RL != -1) {                             //si es atómico
                                        if (resultadoMem == RL) {
                                            cache[(resultadoMem % 16) / 4][((resultadoMem / 4) / 4) % 8] = 1;
                                        }
                                    }
                                } else {
                                    resolverFalloDeCache(resultadoMem);
                                    if (RL != -1) {                             //si es atómico
                                        if (resultadoMem == RL) {
                                            cache[(resultadoMem % 16) / 4][((resultadoMem / 4) / 4) % 8] = 1;
                                        }
                                    }
                                }
                            } catch (java.lang.ArithmeticException exc) {

                                if (hitDeEscritura(resultadoMem)) {
                                    if (RL != -1) {                             //si es atómico
                                        if (resultadoMem == RL) {
                                            cache[0][0] = 1;
                                        }
                                    }
                                } else {
                                    resolverFalloDeCache(resultadoMem);
                                    if (RL != -1) {                             //si es atómico
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

                        cambioEtapa(3);                                         //se pasa la intruccion de MEM a WB
                        resultadoMW = resultadoMem;                            // Mem le pasa el resultado a WB (resultadoMW)

                        sem[3].release();
                        sem[2].release();
                        try {
                            barrier.await();
                            barrier.await();                                    // Este await es para que el hilo general pueda hacer una actualización entre esperas
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                    }
                    
                   // Para poder coordinar correctamente que todas las etapas terminen en orden,
                   // esto para que el hilo principal tenga el control de que se esta ejecutando
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
        
        /**
         * Hilo WB del pipeline: Escribe el resultado en los registros ya sea que
         * venga de un LOAD o del ALU
         */
        final Runnable writeBack = new Runnable() {
            public void run() {
                while (HilosCompletados < numHilos) {
                    while (banderaFin[4] == 0) {
                        int opCode = instruccionWB[0];
                        int op1 = instruccionWB[1];
                        int op2 = instruccionWB[2];
                        int op3 = instruccionWB[3];
                        int regDestino = instruccionWB[4];

                        if ((instruccionWB[0] == 63) || (instruccionWB[0] == 64)) {
                            if (instruccionWB[0] == 64) {
                                banderaFin[4] = 2;

                            } else {
                                banderaFin[4] = 1;
                                colaDeEjecucion[hiloEnEjecucion] = 1;
                                HilosCompletados += 1;
                            }
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
                        if (opCode == 51) {                                     //sc
                            if (RL != -1) {                                     //si es atómico
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
                            barrier.await();                                    // Este await es para que el hilo general pueda hacer una actualización entre esperas
                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (BrokenBarrierException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        };
        /**
         * Hilo principal que lleva el control del reloj, del quantum de cada instruccion
         * Maneja la sincronización con todos los hilos al saber si ya un hilo termino de 
         * ejecutarse y todo el manejo de banderas que esto implica
         */
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
						relojInicial++;
						
                        if (contadorQuantum == 0) {
                            for (int g = 0; g < 4; g++) {
                                instruccionIF[g] = 64;
                            }
                        }
                        
                        if(instruccionWB[0] == 0){
                            salida = 0;                   
                        }

                        if (banderaFin[4] == 1 || banderaFin[4] == 2) {
							relojProcesos[hiloEnEjecucion] += relojInicial;
							relojInicial = 0;
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
                
                imprimirCache();
                cacheAMemoria();
                imprimirVecDatos();
                imprimeResultados();

                System.out.println("El valor del reloj es: " + clock);
                System.exit(clock);
            }
        };

        new Thread(mainThread).start();
    }
 
  /*****************************************************************************
   *                   Implementacion de las  Operaciones 
   *****************************************************************************/
    /**
     * Permite hacer la operacion de DADDI entre los operadores de la instruccion
     * @param ry
     * @param rx
     * @param n
     * @return 
     */ 
    static int daddi(int ry, int rx, int n) {
        int resultado = registros[ry] + n;
        return resultado;
    }

    /**
     * Realiza la operacion del DADD entre los operadores de la instruccion
     * @param ry
     * @param rz
     * @param rx
     * @return 
     */
    static int dadd(int ry, int rz, int rx) {
        int resultado = registros[ry] + registros[rz];
        return resultado;
    }

    /**
     * Realiza la operacion del DSUB entre los operadores de la instruccion
     * @param ry
     * @param rz
     * @param rx
     * @return 
     */
    static int dsub(int ry, int rz, int rx) {
        int resultado = registros[ry] - registros[rz];
        return resultado;
    }
    /**
     * Implementa la operacion de DMUL entre los operadores de la instruccion
     * @param ry
     * @param rz
     * @param rx
     * @return 
     */
    static int dmul(int ry, int rz, int rx) {
        int resultado = registros[ry] * registros[rz];
        return resultado;
    }
    /**
     * Realiza la operacion de DDIV entre los operadores de la instruccion
     * @param ry
     * @param rz
     * @param rx
     * @return 
     */
    static int ddiv(int ry, int rz, int rx) {
        int resultado = registros[ry] / registros[rz];
        return resultado;
    }
    /**
     * Implementa el load con los registros dados en la instruccion 
     * @param ry
     * @param rx
     * @param n
     * @return 
     */
    static int lw(int ry, int rx, int n) {
        int resultado = n + registros[ry] - 768;
        return resultado;
    }

    /**
     * Implementa el Store con los registros dados en la instruccion
     * @param ry
     * @param rx
     * @param n
     * @return 
     */
    static int sw(int ry, int rx, int n) {
        int resultado = n + registros[ry] - 768;
        return resultado;
    }
 
    /**
     * Ejecuta el Branch del hilo (not equal zero)
     * @param rx
     * @param label
     * @param n
     * @return 
     */
    static int bnez(int rx, int label, int n) {
        int resultado = -1;
        return resultado;
    }
    /**
     * Ejecuta el Branch del hilo (equals zero)
     * @param rx
     * @param label
     * @param n
     * @return 
     */
    static int beqz(int rx, int label, int n) {
        int resultado = -1;
        return resultado;
    }
    /**
     * Implementa el salto condicional Jump and link
     * @param n
     * @return 
     */
    static int jal(int n) {
        int resultado = n;
        return resultado;
    }
    /**
     * Implementa la funcionalidad del JUMP REGISTER
     * @param rx
     * @return 
     */
    static int jr(int rx) {
        int resultado = rx;
        return resultado;
    }
    /**
     * Metodo auxiliar del para ejecutar el Load Link
     * @param rx
     * @param n
     * @return 
     */
    static int ll(int rx, int n) {
        int resultado = n + registros[rx] - 768;
        return resultado;
    }
    /**
     * Metodo auxiliar del para ejecutar el Store Conditional
     * @param rx
     * @param n
     * @return 
     */
    static int sc(int rx, int n) {
        int resultado = n + registros[rx] - 768;
        return resultado;
    }
    
    /***************************************************************************
     *                  Metodos Auxiliares
     ***************************************************************************/
    
    /**
     * Se crean las estructuras para el cambio de contexto en las cuales
     * se van a guardar los valores actuales de los registros que estan aun en ejecucion
     * 
     * @param cant 
     */
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
    /**
     * Metodo para solicitar al usuario los hilos que desea ejecutar.
     */
    static void cargarInstrucciones() {
        try {
            JFileChooser loadEmp = new JFileChooser();                          //new dialog
            File[] seleccionados;                                               //needed*
            BufferedReader bf;                                                  //needed*
            int i = 0;
            String linea = "";

            File directorio = new File(System.getProperty("user.dir"));
            loadEmp.setCurrentDirectory(directorio);
            loadEmp.setMultiSelectionEnabled(true);
            loadEmp.showOpenDialog(null);
            seleccionados = loadEmp.getSelectedFiles();
            
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
            if (i % 4 == 0)               //Si es múltiplo de 4       
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
        System.out.println("MEMORIA :");
        for (int i = 0; i < datos.length; i++) {
            if (i % 10 == 0)             //Si es múltiplo de 4       
            {
                System.out.println("");   //cambio de linea
            }
            System.out.print(datos[i] + "\t");
        }
        System.out.println("\n");
    }
    /**
     * Metodo para realizar el cambio de etapa en el mips
     * (IF/ID/EX/MEM/WB)
     * @param x 
     */
    static void cambioEtapa(int x) {
        //Este es el caso en que se mata la instrucción en IF y se pasa una operación vacía a ID
        if (x == -5) {                      
            for (int i = 0; i < 5; i++) {
                instruccionID[i] = 0;
            }
        //Este es el caso en que se mata la instrucción en ID y se pasa una operación vacía a EX          
        } else if (x == -1) {
            for (int i = 0; i < 5; i++) {
                instruccionEX[i] = 0;
            }
        //Este es el caso en que se pasa la intruccion de IF a ID
        } else if (x == 0) {
            for (int i = 0; i < 4; i++) {
                instruccionID[i] = instruccionIF[i];
            }
        //Este es el caso en que se pasa la intruccion de ID a EX
        } else if (x == 1) {
            for (int i = 0; i < 5; i++) {
                instruccionEX[i] = instruccionID[i];
            }
        //Este es el caso en que se pasa la intruccion de EX a MEM
        } else if (x == 2) {
            for (int i = 0; i < 5; i++) {
                instruccionMEM[i] = instruccionEX[i];
            }
        //Este es el caso en que se pasa la intruccion de MEM a WB
        } else if (x == 3) {
            for (int i = 0; i < 5; i++) {
                instruccionWB[i] = instruccionMEM[i];
            }
        }
    }

    static void imprimirCache() {
         System.out.println("Cache de datos:");
        for (int i = 0; i < 6; i++) {
            for (int j = 0; j < 8; j++) {
                System.out.print(cache[i][j] + "\t");
            }
            System.out.print("\n");
        }
        System.out.println();
    }
	
    /**
     *  Este metodo me indica si el bloque que necesito se encuentra en cache
     *  devuelve true si el bloque esta modificado o compartido
     * @param dir
     * @return 
     */
    static boolean hitDeEscritura(int dir) {
        boolean x = false;

        int bloque = (((dir) / 4) / 4) % 8;
        if (cache[4][bloque] == ((dir / 4) / 4)) {  // si el bloque está en caché

            if (cache[5][bloque] == 1) {            //modificado
                x = true;
            }

            if (cache[5][bloque] == 2) {            //compartido
                x = true;
            }

        }

        return x;
    }
    
    /**
     * Este metodo resuelve los fallos de cache, esto ocurre cuando necesito subir un bloque a cache y la posicion en la que le corresponde 
     * esta siendo ocupada por otro bloque y este posee un estado modificado, por lo cual primero se debera de pasar este bloque a memoria para 
     * luego copiar el nuevo que necesito
     * @param dir 
     */
    static void resolverFalloDeCache(int dir) {
        //System.out.println("Antes del Fallo \n");

        System.out.println("\n");
        int bloque = (((dir) / 4) / 4) % 8;
        // En este caso el bloque esta modificado 
        if ((cache[5][bloque] == 1)) { 

            for (int i = 0; i < 4; i++) {
                datos[(cache[4][bloque] * 4) + i] = cache[i][bloque];
            }

        }

        for (int i = 0; i < 4; i++) {                                           //Sube los datos de memoria a cache
            cache[i][bloque] = datos[(((dir / 4) / 4) * 4) + (i)];              // :)
        }
        cache[4][bloque] = ((dir) / 4) / 4;                                     //cambia la etiqueta
        cache[5][bloque] = 2;                                                   // estado = compartido
    }

	static void imprimeResultados() {
        FileWriter fichero = null;
        PrintWriter pw = null;

        for (int i = 0; i < numHilos; i++) {
            String nombre = "Resultado-Hilo-" + i + ".txt";

            try {
                fichero = new FileWriter(nombre);
                pw = new PrintWriter(fichero);

                pw.println("Reloj del Hilo " + i + " = " + relojProcesos[i]);
                pw.println("");

                for (int j = 0; j < 32; j++) {
                    pw.println("R" + j + " = " + regProcesos[j][i]);
                }
                pw.println("");

                pw.println("RL = " + RLProcesos[i]);

                fichero.close();
            } catch (IOException e) {
            }
        }

        try {
            fichero = new FileWriter("Resultados-Memoria-Cache.txt");
            pw = new PrintWriter(fichero);

            pw.println("Cache de datos:");
            for (int i = 0; i < 6; i++) {
                for (int j = 0; j < 8; j++) {
                    pw.print(cache[i][j] + "\t");
                }
                pw.println();
            }
            pw.println();

            pw.println("MEMORIA :");
            for (int i = 0; i < datos.length; i++) {
                if (i % 10 == 0) //Si es múltiplo de 10       
                {
                    pw.println("");   //cambio de linea
                }
                pw.print(datos[i] + "\t");
            }
            pw.println();
            pw.println();

            pw.println("El valor del reloj final de ejecucion es: " + clock);
            
            fichero.close();
        } catch (IOException e) {
        }

    }
	
    /**
     * Este metodo copia un registro de cache a memoria, esto se utiliza cuando ocurre un fallo de cache 
     */
    static void cacheAMemoria() {
        for (int p = 0; p < 8; p++) {
            for (int i = 0; i < 4; i++) {
                if (cache[5][p] == 1) {                                         //si está modificado
                    datos[(cache[4][p] * 4) + i] = cache[i][p];
                }
            }
        }

    }

    /**
     * Metdodo para solicitarle al usuario el quantum (Preferiblemente usar un quantum mayor que 100)
     */
    static void pedirQuantum() {
        try {
            JFrame frame = new JFrame("¥*¥");
            String name = JOptionPane.showInputDialog(frame, "Cual es el quantum? Por favor ingrese un numero sino no saldra de aqui.");
            quantum = Integer.parseInt(name);
        } catch (Exception e) {
            pedirQuantum();
        }

    }

    /**
     * Se utiliza cuando el quantum ingresado por el usuario se ha terminado
     */
    static void cambioDeContexto() {
        boolean si = false;

        for (int i = 0; i < 32; i++) {                                          // se copia el valor de los registros actuales a la estructura de datos
            regProcesos[i][hiloEnEjecucion] = registros[i];
        }
        PCProcesos[hiloEnEjecucion] = pc;                                       // se copia el pc actual
        RLProcesos[hiloEnEjecucion] = -1;

        for (int n = 1; n <= numHilos; n++) {
            if (!si && colaDeEjecucion[(hiloEnEjecucion + n) % numHilos] == 0) {
                hiloEnEjecucion = (hiloEnEjecucion + n) % numHilos;
                si = true;
            }
        }

        if (!si) {
            hiloEnEjecucion = -1;                                               // el hilo ya termino de ejecutarse
        }

        if (si) {                                                               // significa que no ha terminado de ejecutarse.
            for (int i = 0; i < 32; i++) {
                registros[i] = regProcesos[i][hiloEnEjecucion];
            }
            pc = PCProcesos[hiloEnEjecucion];                                   //se le asigna al pc del programa el valor del pc del  hilo que le toca ejecutarse
            RL = RLProcesos[hiloEnEjecucion];

        }
        //Sirve para limpiar las estructuras y de esta forma futuros conflictos
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
