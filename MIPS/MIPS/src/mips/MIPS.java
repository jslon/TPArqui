/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mips;

import java.lang.Thread;
import java.io.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class MIPS {

    static int clock = 0;
    static int[] datos = new int[200];
    static int[] instrucciones = new int[400];
    static int[] registros = new int[32];
    static int[] instruccionIF = new int[4];
    static int[] instruccionID = new int[5];
    static int[] instruccionEX = new int[5];
    static int[] instruccionMEM = new int[5];
    static int[] instruccionWB = new int[5];
    static int pc = 0;
    static int[] tablaReg = new int[32];
    static int[] banderaFin = new int[5];                     // indica la finalización del programa para cada etapa. 1 = FIN
    static int resultadoEM = 0;                              // EX le pasa el resultado a Mem
    static int resultadoMem = 0;                              // es el resultado para lw y sw
    static int valMemoriaLW = 0;
    static int resultadoMW = 0;                              // de Men a Wb 
    private static Semaphore[] sem = new Semaphore[]{new Semaphore(1), new Semaphore(1), new Semaphore(1), new Semaphore(1)};
    private static Semaphore semReg = new Semaphore(1);
    static CyclicBarrier barrier = new CyclicBarrier(6);

    static CyclicBarrier IFaID = new CyclicBarrier(2);
    static CyclicBarrier IDaEX = new CyclicBarrier(2);
    static CyclicBarrier EXaMEM = new CyclicBarrier(2);
    static CyclicBarrier MEMaWB = new CyclicBarrier(2);

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
                    /*try {
                        //mientras no tenga que finalizar

                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }*/

                    System.out.println("PC: " + pc);

                    // Copia la instrucción de la "memoria" al vector de instrucción de IF
                    for (int i = 0; i < 4; i++) {
                        instruccionIF[i] = instrucciones[(pc) + i];
                    }

                    pc += 4;

                    System.out.println("Instruccion en IF:\t");
                    for (int i = 0; i < 4; i++) {
                        System.out.print(instruccionIF[i] + "\t");
                    }
                    

                    if (instruccionIF[0] == 63) //Si la instrucción es FIN
                    {
                        banderaFin[0] = 1;
                    }
                    
                    try {
                        sem[0].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    cambioEtapa(0);

                    sem[0].release(1);
                    try {
                        barrier.await();
                        barrier.await();                // Este await es para que el hilo general pueda hacer una actualización entre esperas
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

                while (banderaFin[4] == 0) {
                    try {
                        //barrier.await();
                        barrier.await();
                        System.out.println("Espero a WB\t");
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }
        };

        final Runnable instructionDecode = new Runnable() {
            public void run() {
                while (banderaFin[1] == 0) {

                    /*try {
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }*/

                    int opCode = instruccionID[0];
                    int op1 = registros[instruccionID[1]];
                    int op2 = registros[instruccionID[2]];
                    int op3 = instruccionID[3];
                    
                    
                    //hay que hacer lógica condicional, depende del opcode los op1 y op2 van a ser op ó registros[op]

                    System.out.println("Instruccion en ID:\t");
                    for (int i = 0; i < 4; i++) {
                        System.out.print(instruccionID[i] + "\t");
                    }
                    System.out.println("");

                    if (opCode == 63) {
                        cambioEtapa(1);
                        banderaFin[1] = 1;
                    }

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

                    //switch que identifique el OP
                    if (opCode == 8) {
                        if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0) {  //Si los registros op1 y op2 están libres
                            instruccionID[4] = op2;
                            cambioEtapa(1);
                        }
                    }
                    if (opCode == 35) {
                        if (tablaReg[instruccionID[2]] == 0) {	//Si el registro op2 está libre
                            instruccionID[4] = op2;
                            cambioEtapa(1);
                        }
                    }
                    if (opCode == 32) {
                        if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                            instruccionID[4] = op3;
                            cambioEtapa(1);
                        }
                    }
                    if (opCode == 12) {
                        if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                            instruccionID[4] = op3;
                            cambioEtapa(1);
                        }
                    }
                    if (opCode == 14) {
                        if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                            instruccionID[4] = op3;
                            cambioEtapa(1);
                        }
                    }
                    if (opCode == 34) {
                        if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                            instruccionID[4] = op3;
                            cambioEtapa(1);
                        }
                    }
                    if (opCode == 43) {
                        if (tablaReg[instruccionID[1]] == 0 && tablaReg[instruccionID[2]] == 0) {  //Si los registros op1 y op2 están libres
                            cambioEtapa(1);
                        }
                    }
                    if (opCode == 4){                           //BEQZ
                    }
                    if (opCode == 5){                           //BNEZ
                    }
                    if (opCode == 3){                           //JAL
                    }
                    if (opCode == 2){                           //JR
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

                while (banderaFin[4] == 0) {
                    try {
                        //barrier.await();
                        barrier.await();
                        System.out.println("Espero a WB\t");
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }

        };

        final Runnable execute = new Runnable() {
            public void run() {
                while (banderaFin[2] == 0) {

                    /*try {
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }*/

                    int opCode = instruccionEX[0];
                    int op1 = instruccionEX[1];
                    int op2 = instruccionEX[2];
                    int op3 = instruccionEX[3];
                    int regDestino = instruccionEX[4];
                    int resultado = 0;

                    System.out.println("Instruccion en EX:\t");
                    for (int i = 0; i < 4; i++) {
                        System.out.print(instruccionEX[i] + "\t");
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
                        resultado = beqz(op1, op2, op3);
                    }
                    if (opCode == 5) {                  //BNEZ
                        resultado = bnez(op1, op2, op3);
                    }
                    if (opCode == 3) {                  //JAL
                        resultado = jal(op3);
                    }
                    if (opCode == 2) {                  //JR
                        resultado = jr(op1);
                    }
                    if (opCode == 63) {                  //FIN
                        banderaFin[2] = 1;
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

                while (banderaFin[4] == 0) {
                    try {
                       // barrier.await();
                        barrier.await();
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }
        };

        final Runnable memory = new Runnable() {
            public void run() {
                while (banderaFin[3] == 0) {

                    /*try {
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }*/

                    int opCode = instruccionMEM[0];
                    int op1 = instruccionMEM[1];
                    int op2 = instruccionMEM[2];
                    int op3 = instruccionMEM[3];
                    int regDestino = instruccionMEM[4];
                    valMemoriaLW = resultadoEM;

                    System.out.println("Instruccion en MEM:\t");
                    for (int i = 0; i < 4; i++) {
                        System.out.print(instruccionMEM[i] + "\t");
                    }
                    System.out.println("");

                    if (opCode == 63) {
                        banderaFin[3] = 1;
                    }

                    if (opCode == 35 || opCode == 43) {		//Si la instrucción es LW o SW
                        resultadoMem = resultadoEM;
                        if (opCode == 43) {	// este puede escribir
                            datos[resultadoMem] = op2;/*registros[op2];*/ //se le guarda el valor del registros
                        } else { 							//saca el valor de memoria y lo guarda en esta variable
                            valMemoriaLW = datos[resultadoMem];
                        }
                    }
                    else {                      //Si es cualquier otra intruccion
                    
                    }
                    
                    try {
                        sem[3].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    cambioEtapa(3);
                    resultadoMW = valMemoriaLW;

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

                while (banderaFin[4] == 0) {
                    try {
                     //   barrier.await();
                        barrier.await();
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
            }
        };

        final Runnable writeBack = new Runnable() {
            public void run() {                
                while (banderaFin[4] == 0) {
                    
                    /*try {
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }*/

                    int opCode = instruccionWB[0];
                    int op1 = instruccionWB[1];
                    int op2 = instruccionWB[2];
                    int op3 = instruccionWB[3];
                    int regDestino = instruccionWB[4];

                    System.out.println("Instruccion en WB:\t");
                    for (int i = 0; i < 4; i++) {
                        System.out.print(instruccionWB[i] + " ");
                    }

                    if (opCode == 63) {
                        banderaFin[4] = 1;
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
                    System.out.print("destino: "+instruccionWB[4]+"resultadoMW: "+resultadoMW);

                    //Liberacion de los registros
                    if (opCode == 8) {
                        tablaReg[op1] = 0;
                        tablaReg[op2] = 0;
                    }
                    if (opCode == 35) {
                        tablaReg[op1] = 0;
                        tablaReg[op2] = 0;
                    }
                    if (opCode == 32) {
                        tablaReg[op1] = 0;
                        tablaReg[op2] = 0;
                        tablaReg[op3] = 0;
                    }
                    if (opCode == 12) {
                        tablaReg[op1] = 0;
                        tablaReg[op2] = 0;
                        tablaReg[op3] = 0;
                    }
                    if (opCode == 14) {
                        tablaReg[op1] = 0;
                        tablaReg[op2] = 0;
                        tablaReg[op3] = 0;
                    }
                    if (opCode == 34) {
                        tablaReg[op1] = 0;
                        tablaReg[op2] = 0;
                        tablaReg[op3] = 0;
                    }
                    if (opCode == 43) {
                    }

                    sem[3].release(1);
                    semReg.release();

                    //System.out.println("llegué a WB");

                    try {
                        barrier.await();
                        barrier.await();                // Este await es para que el hilo general pueda hacer una actualización entre esperas
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }
                
                /*try {
                      //  barrier.await();
                        barrier.await();
                        barrier.await();                // Este await es para que el hilo general pueda hacer una actualización entre esperas
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }*/
                
            }
        };

        Runnable mainThread;
        mainThread = new Runnable() {
            public void run() {

                cargarInstrucciones();

                //imprimirVecInstrucciones();
                //System.out.print("\n\n");
                

                // inicia los hilos
                new Thread(instructionFetch).start();
                new Thread(instructionDecode).start();
                new Thread(execute).start();
                new Thread(memory).start();
                new Thread(writeBack).start();

                while (banderaFin[0] == 0 || banderaFin[1] == 0 || banderaFin[2] == 0 || banderaFin[3] == 0 || banderaFin[4] == 0) {
                    try {
                        //barrier.await();
                        barrier.await();
                        
                        sem[0].drainPermits();
                        sem[1].drainPermits();
                        sem[2].drainPermits();
                        sem[3].drainPermits();

                        
                        try {
                            /*
                            if(sem[0].availablePermits() == 1)
                            {sem[0].acquire();}
                            if(sem[1].availablePermits() == 1)
                            {sem[1].acquire();}
                            if(sem[2].availablePermits() == 1)
                            {sem[2].acquire();}
                            if(sem[3].availablePermits() == 1)
                            {sem[3].acquire();}
                                */
                            semReg.acquire();

                            for (int i = 0; i < 4; i++) {
                                System.out.print("Semaforo " + i + ":" + sem[1].availablePermits() + " ");
                            }
                            System.out.println("");

                        } catch (InterruptedException ex) {
                            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                        }

                        clock++;
                        barrier.await();
                        
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                }

                imprimirVecDatos();
                imprimirRegistros();
                System.exit(clock);
            }
        };

        new Thread(mainThread).start();
    }

    //Operaciones 
    static int daddi(int ry, int rx, int n) {
        int resultado = ry + n;
        return resultado;
    }

    static int dadd(int ry, int rz, int rx) {
        int resultado = ry + rz;
        return resultado;
    }

    static int dsub(int ry, int rz, int rx) {
        int resultado = ry - rz;
        return resultado;
    }

    static int dmul(int ry, int rz, int rx) {
        int resultado = ry * rz;;
        return resultado;
    }

    static int ddiv(int ry, int rz, int rx) {
        int resultado = ry / rz;;
        return resultado;
    }

    static int lw(int ry, int rx, int n) {
        int resultado = n + ry;
        return resultado;
    }

    static int sw(int ry, int rx, int n) {
        int resultado = n + ry;
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

    static void cargarInstrucciones() {
        try {

            BufferedReader bf = new BufferedReader(new FileReader("HILO-B-v2.txt"));
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
            Logger.getLogger(MIPS.class
                    .getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void imprimirVecInstrucciones() {
        for (int i = 0; i < instrucciones.length; i++) {
            if (i % 4 == 0) //Si es múltiplo de 4       
            {
                System.out.print("\n");   //cambio de linea
            }
            System.out.print(Integer.toString(instrucciones[i]) + "\t");

        }
    }

    static void imprimirRegistros() {
        for (int i = 0; i < registros.length; i++) {
            System.out.println("R" + i + ": " + registros[i] + " ");
        }
    }

    static void imprimirVecDatos() {
        for (int i = 0; i < datos.length; i++) {
            if (i % 4 == 0) //Si es múltiplo de 4       
            {
                System.out.print("\n");   //cambio de linea
            }
            System.out.print(datos[i] + "\t");

        }
    }

    static void cambioEtapa(int x) {
        if (x == 0) {
            for (int i = 0; i < 4; i++) {
                instruccionID[i] = instruccionIF[i];
            }
        }
        else if (x == 1) {
            for (int i = 0; i < 5; i++) {
                instruccionEX[i] = instruccionID[i];
            }
        }
        else if (x == 2) {
            for (int i = 0; i < 5; i++) {
                instruccionMEM[i] = instruccionEX[i];
            }
        }
        else if (x == 3) {
            for (int i = 0; i < 5; i++) {
                instruccionWB[i] = instruccionMEM[i];
            }
        }
    }

}
