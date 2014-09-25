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

    static int   clock = 0;
    static int[] datos          = new int[200];
    static int[] instrucciones  = new int[400];
    static int[] registros      = new int[32];
    static int[] instruccionIF  = new int[4];
    static int[] instruccionID  = new int[4];
    static int[] instruccionEX  = new int[4];
    static int[] instruccionMEM = new int[4];
    static int[] instruccionWB  = new int[4];
    static int   pc             = 0;
    static int[] tablaReg       = new int[32];                    
    static int[] banderaFin     = new int[5];                     // indica la finalización del programa para cada etapa. 1 = FIN
    static int   resultadoEM    = 0;                              // EX le pasa el resultado a Mem
    static int   resultadoMem   = 0;                              // es el resultado para lw y sw
    static int   valMemoriaLW   = 0;
    static int   resultadoMW    = 0;                              // de Men a Wb 
    private static Semaphore[] sem          = new Semaphore[]
        {new Semaphore(1), new Semaphore(1), new Semaphore(1), new Semaphore(1)};
    private static Semaphore   semGeneral   = new Semaphore(0);
    static CyclicBarrier barrier = new CyclicBarrier(5);
    
    
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
                while (banderaFin[0] == 0) {        //mientras no tenga que finalizar
                    
                    System.out.println("PC: "+pc);
                    
                    
                    // Copia la instrucción de la "memoria" al vector de instrucción de IF
                    for (int i = 0; i < 4; i++) {
                        instruccionIF[i] = instrucciones[(pc * 4) + i];
                    }
                    
                    pc++;
                    
                    System.out.println("Instruccion en IF:\t");
                    for (int i =0; i<4; i++) {
                        System.out.println(instruccionIF[i]+"\t");
                    }
                    
                    
                    if (instruccionIF[0] == 63)	//Si la instrucción es FIN
                        banderaFin[0] = 1;
                    
                    System.out.println("llegue aqui :)");
                    try {
                        sem[0].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    
                    // Copia la intrucción al vector de instrucción de la siguente etapa
                    cambioEtapa(0);

                    
                    
                     sem[0].release();
                    
                    try {
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                   

                    /*
                    try {
                        semGeneral.acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    */
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

                    int opCode	= instruccionID[0];
                    int op1 	= instruccionID[1];
                    int op2 	= instruccionID[2];
                    int op3 	= instruccionID[3];
                    
                    System.out.println("Instruccion en ID:\t");
                    for (int i =0; i<4; i++) {
                        System.out.println(instruccionID[i]+"\t");
                    }

                    if (instruccionID[0] == 63)
                        banderaFin[0] = 1;

                    try {
                        sem[1].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    //switch que identifique el OP
                    switch (opCode) {
                        case (8): {                   	//DADDI
                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0) {  //Si los registros op1 y op2 están libres
                                cambioEtapa(2);
                            }
                        }

                        case (32): {     				//DADD

                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                                cambioEtapa(2);
                            }
                        }

                        case (34): {     				//DSUB

                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                                cambioEtapa(2);
                            }
                        }

                        case (12): {     				//DMUL

                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                                cambioEtapa(2);
                            }
                        }

                        case (14): {     				//DDIV

                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0 && tablaReg[op3] == 0) {  //Si los registros op1,op2, op3 están libres
                                cambioEtapa(2);
                            }
                        }
                        
                        case 35: {                  	//LW
                            if (tablaReg[op2] == 0) {	//Si el registro op2 está libre
                                cambioEtapa(2);
                            }
                        }

                        case (43): {                   	//SW
                            if (tablaReg[op1] == 0 && tablaReg[op2] == 0) {  //Si los registros op1 y op2 están libres
                                cambioEtapa(2);
                            }
                        }

                        
                        case 4: {                  		//BEQZ

                        }
                        case 5: {                  		//BNEZ

                        }
                        case 3: {                  		//JAL

                        }
                        case 2: {                  		//JR

                        }
                        case 63: {                  	//FIN
                            //Avisa que esta terminando
                        }
                    }
                    System.out.println("llegué a ID");
                    
                    sem[0].release();
                    sem[1].release();
                    
                    
                    
                    try {
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    

                    /*
                    try {
                        semGeneral.acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    */
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

                    int opCode          = instruccionEX[0];
                    int op1 		= instruccionEX[1];
                    int op2 		= instruccionEX[2];
                    int op3 		= instruccionEX[3];
                    int resultado 	= 0;
                    
                    System.out.println("Instruccion en EX:\t");
                    for (int i =0; i<4; i++) {
                        System.out.println(instruccionEX[i]+"\t");
                    }

                    if (instruccionEX[0] == 63)
                        banderaFin[0] = 1;

                    switch (opCode) {
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

                    cambioEtapa(2);

                    resultadoEM = resultado;
                    
                    sem[1].release();
                    sem[2].release();
                    
                    try {
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    

                     /*
                    try {
                        semGeneral.acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    */
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

                    int opCode		= instruccionMEM[0];
                    int op1             = instruccionMEM[1];
                    int op2		= instruccionMEM[2];
                    int op3		= instruccionMEM[3];
                    valMemoriaLW 	= resultadoEM;
                    
                    System.out.println("Instruccion en MEM:\t");
                    for (int i =0; i<4; i++) {
                        System.out.println(instruccionMEM[i]+"\t");
                    }

                    if (opCode == 63)
                        banderaFin[0] = 1;

                    if (opCode == 35 || opCode == 43) {		//Si la instrucción es LW o SW
                        resultadoMem = resultadoEM;
                        if (opCode == 43) {	// este puede escribir
                            datos[resultadoMem * 4] = op2; //se le guarda el valor del registros
                        }
                        else { 							//saca el valor de memoria y lo guarda en esta variable
                            valMemoriaLW = datos[resultadoMem * 4];
                        }
                    }

                    
                    System.out.println("llegué a ID");
                    try {
                        sem[3].acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }

                    cambioEtapa(3);
                    resultadoMW = valMemoriaLW;

                    sem[3].release();
                    sem[4].release();
                    
                    try {
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    
                    
                    
                    /*
                    try {
                        semGeneral.acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    */

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

                    int opCode 	= instruccionWB[0];
                    int op1 	= instruccionWB[2];
                    int op2 	= instruccionWB[2];
                    int op3 	= instruccionWB[3];
                    
                    System.out.println("Instruccion en WB:\t");
                    for (int i =0; i<4; i++) {
                        System.out.println(instruccionWB[i]+"\t");
                    }

                    if (opCode == 63)
                        banderaFin[0] = 1;

                    if (opCode == 8 || opCode == 35) {
                        registros[op2] = resultadoMW;
                    }
                    else {
                        registros[op3] = resultadoMW;			// las operaciones aritmeticas de add, sub, mul y ddiv  		***********(¿Qué pasa con el 63?)**************
                    }
                    //Liberacion de los registros
                    if (opCode == 8 || opCode == 35) {
                        tablaReg[op1] = 0;
                        tablaReg[op2] = 0;
                    } 
                    else { // las operaciones aritmeticas de add, sub, mul y div
                        tablaReg[op1] = 0;
                        tablaReg[op2] = 0;
                        tablaReg[op3] = 0;
                    }
                    
                    sem[4].release();
                    
                    System.out.println("llegué a WB");
                    
                    try {
                        barrier.await();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (BrokenBarrierException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                   
                    
                    /*
                    try {
                        semGeneral.acquire();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
                    }
                            */

                }
            }
        };

        Runnable mainThread = new Runnable() {
            public void run() {
                
                
                cargarInstrucciones();
                
                imprimirVecInstrucciones();
                System.out.print("\n\n");
                
                // inicia los hilos
                new Thread(writeBack).start();
                new Thread(memory).start();
                new Thread(execute).start();
                new Thread(instructionDecode).start();
                new Thread(instructionFetch).start();

                  
                while(banderaFin[0] == 0 && banderaFin[1] == 0 && banderaFin[2] == 0 && banderaFin[3] == 0 && banderaFin[4] == 0) { 
                    
                }

                imprimirVecDatos();
                System.exit(clock);
            }
        };
        
        new Thread(mainThread).start();
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
            Logger.getLogger(MIPS.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    static void imprimirVecInstrucciones(){
        for (int i = 0; i < instrucciones.length; i++) {
            if(i%4 == 0)               //Si es múltiplo de 4       
                    System.out.print("\n");   //cambio de linea
            System.out.print(Integer.toString(instrucciones[i])+"\t");
                
        }
    }

    static void imprimirVecDatos(){
        for (int i = 0; i < datos.length; i++) {
             if(i%4 == 0)               //Si es múltiplo de 4       
                    System.out.print("\n");   //cambio de linea
            System.out.print(Integer.toString(datos[i])+"/t");
               
        }
    }

    static void cambioEtapa(int x) {
    	switch (x) {

    		//IF a ID
    		case 0:
    			for (int i = 0; i < 4; i++) {
                	instruccionID[i] = instruccionIF[i];
                }

    		// ID a EX
    		case 1:
    			for (int i = 0; i < 4; i++) {
                	instruccionEX[i] = instruccionID[i];
                }

    		// EX a MEM
    		case 2:
    			for (int i = 0; i < 4; i++) {
                	instruccionMEM[i] = instruccionEX[i];
                }

    		// MEM a WB
    		case 3:
    			for (int i = 0; i < 4; i++) {
            		instruccionWB[i] = instruccionMEM[i];
            	}
    	}

    }

}
