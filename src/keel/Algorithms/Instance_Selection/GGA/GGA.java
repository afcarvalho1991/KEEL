/***********************************************************************

	This file is part of KEEL-software, the Data Mining tool for regression, 
	classification, clustering, pattern mining and so on.

	Copyright (C) 2004-2010
	
	F. Herrera (herrera@decsai.ugr.es)
    L. Sánchez (luciano@uniovi.es)
    J. Alcalá-Fdez (jalcala@decsai.ugr.es)
    S. García (sglopez@ujaen.es)
    A. Fernández (alberto.fernandez@ujaen.es)
    J. Luengo (julianlm@decsai.ugr.es)

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see http://www.gnu.org/licenses/
  
**********************************************************************/



package keel.Algorithms.Instance_Selection.GGA;

import keel.Algorithms.Preprocess.Basic.*;

import org.core.*;
import java.util.StringTokenizer;
import java.util.Arrays;

/**
 * 
 * File: GGA.java
 * 
 * Generational Genetic algorithm for Instance Selection.
 * 
 * @author Written by Salvador García (University of Granada) 20/07/2004 
 * @version 0.1 
 * @since JDK1.5
 * 
 */
public class GGA extends Metodo {

	/*Own parameters of the algorithm*/
	private long semilla;
	private double pMutacion1to0;
	private double pMutacion0to1;
	private double pCruce;
	private int tamPoblacion;
	private int nEval;
	private double alfa;
	private boolean torneo;
	private int kNeigh;

    /**
     * Default builder. Construct the algoritm by using the superclass builder.
	 * @param ficheroScript Configuration script
     */
	 public GGA (String ficheroScript) {
		super (ficheroScript);
		
	}//end-method 
	 
	/**
	 * Executes the algorithm
	 */
	public void ejecutar () {

		int i, j, l;
		int nClases;
		double conjS[][];
		double conjR[][];
		int conjN[][];
		boolean conjM[][];
		int clasesS[];
		int nSel = 0;
		Cromosoma poblacion[];
		int ev = 0;
		double prob[];
		double NUmax = 1.5;
		double NUmin = 0.5; //used for lineal ranking
		double aux;
		double pos1, pos2;
		int sel1, sel2, comp1, comp2;
		Cromosoma newPob[];

		long tiempo = System.currentTimeMillis();

		/*Getting the number of different classes*/
		nClases = 0;
		for (i=0; i<clasesTrain.length; i++)
		  if (clasesTrain[i] > nClases)
			nClases = clasesTrain[i];
		nClases++;

		/*Random inicialization of the population*/
		Randomize.setSeed (semilla);
		poblacion = new Cromosoma[tamPoblacion];
		for (i=0; i<tamPoblacion; i++)
		  poblacion[i] = new Cromosoma (datosTrain.length);

		/*Initial evaluation of the population*/
		for (i=0; i<tamPoblacion; i++)
		  poblacion[i].evalua(datosTrain, realTrain, nominalTrain, nulosTrain, clasesTrain, alfa, kNeigh, nClases, distanceEu);

		if (torneo) {
		  while (ev < nEval) {
			/*The size of the population says if Elitism is used or not*/
			newPob = new Cromosoma[tamPoblacion];
			if ((tamPoblacion % 2)==1) l = 1;
			else l = 0;
			for (i=0; i<((tamPoblacion-l)/2); i++) {
			  /*Binary tournament selection*/
			  comp1 = Randomize.Randint(0,tamPoblacion-1);
			  do {
				comp2 = Randomize.Randint(0,tamPoblacion-1);
			  } while (comp2 == comp1);
			  if (poblacion[comp1].getCalidad() > poblacion[comp2].getCalidad())
				sel1 = comp1;
			  else sel1 = comp2;
			  comp1 = Randomize.Randint(0,tamPoblacion-1);
			  do {
				comp2 = Randomize.Randint(0,tamPoblacion-1);
			  } while (comp2 == comp1);
			  if (poblacion[comp1].getCalidad() > poblacion[comp2].getCalidad())
				sel2 = comp1;
			  else sel2 = comp2;

			  if (Randomize.Rand() < pCruce) { //there is cross
				crucePMX (poblacion, newPob, sel1, sel2, i);
			  } else { //there is not cross
				newPob[i*2] = new Cromosoma (datosTrain.length, poblacion[sel1]);
				newPob[i*2+1] = new Cromosoma (datosTrain.length, poblacion[sel2]);
			  }
			}
			if (l == 1) { //Elitism
			  Arrays.sort(poblacion);
			  newPob[tamPoblacion-1] = new Cromosoma (datosTrain.length, poblacion[0]);
			}

			poblacion = newPob;

			/*Mutation of cromosomes*/
			for (i=0; i<tamPoblacion; i++)
			  poblacion[i].mutacion(pMutacion1to0, pMutacion0to1);

			/*Evaluation of the population*/
			for (i=0; i<tamPoblacion; i++)
			  if (!(poblacion[i].estaEvaluado())) {
				poblacion[i].evalua(datosTrain, realTrain, nominalTrain, nulosTrain, clasesTrain, alfa, kNeigh, nClases, distanceEu);
				ev++;
			  }
		  }
		} else {
		  /*Get the probabilities of the lineal ranking in case of not use binary tournament*/
		  prob = new double[tamPoblacion];
		  for (i=0; i<tamPoblacion; i++) {
			aux= (double)( NUmax-NUmin)*((double)i/(tamPoblacion-1));
			prob[i]=(double)(1.0/(tamPoblacion)) * (NUmax-aux);
		  }
		  for (i=1; i<tamPoblacion; i++)
			prob[i] = prob[i] + prob[i-1];

		  while (ev < nEval) {
			/*Sort by quality criterion the population*/
			Arrays.sort(poblacion);

			/*The size of the population says if Elitism is used or not*/
			newPob = new Cromosoma[tamPoblacion];
			if ((tamPoblacion % 2)==1) l = 1;
			else l = 0;
			for (i=0; i<((tamPoblacion-l)/2); i++) {
			  pos1 = Randomize.Rand();
			  pos2 = Randomize.Rand();
			  for (j=0; j<tamPoblacion && prob[j]<pos1; j++);
			  sel1 = j;
			  for (j=0; j<tamPoblacion && prob[j]<pos2; j++);
			  sel2 = j;

			  if (Randomize.Rand() < pCruce) { //there is cross
				crucePMX (poblacion, newPob, sel1, sel2, i);
			  } else { //there is not cross
				newPob[i*2] = new Cromosoma (datosTrain.length, poblacion[sel1]);
				newPob[i*2+1] = new Cromosoma (datosTrain.length, poblacion[sel2]);
			  }
			}
			if (l == 1) { //Elitism
			  newPob[tamPoblacion-1] = new Cromosoma (datosTrain.length, poblacion[0]);
			}

			poblacion = newPob;
			/*Mutation of the cromosomes*/
			for (i=0; i<tamPoblacion; i++)
			  poblacion[i].mutacion(pMutacion1to0, pMutacion0to1);

			/*Evaluation of the population*/
			for (i=0; i<tamPoblacion; i++)
			  if (!(poblacion[i].estaEvaluado())) {
				poblacion[i].evalua(datosTrain, realTrain, nominalTrain, nulosTrain, clasesTrain, alfa, kNeigh, nClases, distanceEu);
				ev++;
			  }
		  }
		}

		Arrays.sort(poblacion);
		nSel = poblacion[0].genesActivos();

		/*Building of S set from the best cromosome obtained*/
		conjS = new double[nSel][datosTrain[0].length];
		conjR = new double[nSel][datosTrain[0].length];
		conjN = new int[nSel][datosTrain[0].length];
		conjM = new boolean[nSel][datosTrain[0].length];
		clasesS = new int[nSel];
		for (i=0, l=0; i<datosTrain.length; i++) {
		  if (poblacion[0].getGen(i)) { //the instance must be copied to the solution
			for (j=0; j<datosTrain[0].length; j++) {
			  conjS[l][j] = datosTrain[i][j];
			  conjR[l][j] = realTrain[i][j];
			  conjN[l][j] = nominalTrain[i][j];
			  conjM[l][j] = nulosTrain[i][j];
			}
			clasesS[l] = clasesTrain[i];
			l++;
		  }
		}

		System.out.println("GGA "+ relation + " " + (double)(System.currentTimeMillis()-tiempo)/1000.0 + "s");

        // COn conjS me vale.
        int trainRealClass[][];
        int trainPrediction[][];
                
         trainRealClass = new int[datosTrain.length][1];
		 trainPrediction = new int[datosTrain.length][1];	
                
         //Working on training
         for ( i=0; i<datosTrain.length; i++) {
              trainRealClass[i][0] = clasesTrain[i];
              trainPrediction[i][0] = KNN.evaluate(datosTrain[i],conjS, nClases, clasesS, this.kNeigh);
          }
                 
          KNN.writeOutput(ficheroSalida[0], trainRealClass, trainPrediction,  entradas, salida, relation);
                 
                 
        //Working on test
		int realClass[][] = new int[datosTest.length][1];
		int prediction[][] = new int[datosTest.length][1];	
		
		//Check  time		
				
		for (i=0; i<realClass.length; i++) {
			realClass[i][0] = clasesTest[i];
			prediction[i][0]= KNN.evaluate(datosTest[i],conjS, nClases, clasesS, this.kNeigh);
		}
                
         KNN.writeOutput(ficheroSalida[1], realClass, prediction,  entradas, salida, relation);

		
	}//end-method 

	/**
	 * PMX cross operator
	 *
	 * @param poblacion Population of chromosomes
	 * @param newPob New population
	 * @param sel1 First parent
	 * @param sel2 Second parent
	 * @param pos Position of the chromosomes in the new population
	 */
	public void crucePMX (Cromosoma poblacion[], Cromosoma newPob[], int sel1, int sel2, int pos) {

		int e1, e2;
		int limSup, limInf;
		int i;
		boolean temp[];

		temp = new boolean[datosTrain.length];
		e1 = Randomize.Randint (0, datosTrain.length-1);
		e2 = Randomize.Randint (0, datosTrain.length-1);
		if (e1 > e2) {
		  limSup = e1;
		  limInf = e2;
		} else {
		  limSup = e2;
		  limInf = e1;
		}

		for (i=0; i<datosTrain.length; i++) {
		  if (i < limInf || i > limSup)
			temp[i] = poblacion[sel1].getGen(i);
		  else temp[i] = poblacion[sel2].getGen(i);
		}
		newPob[pos*2] = new Cromosoma (temp);

		for (i=0; i<datosTrain.length; i++) {
		  if (i < limInf || i > limSup)
			temp[i] = poblacion[sel2].getGen(i);
		  else temp[i] = poblacion[sel1].getGen(i);
		}
		newPob[pos*2+1] = new Cromosoma (temp);
		
	}//end-method 

  	/** 
	 * Reads configuration script, and extracts its contents.
	 * 
	 * @param ficheroScript Name of the configuration script  
	 * 
	 */	
	public void leerConfiguracion (String ficheroScript) {

		String fichero, linea, token;
		StringTokenizer lineasFichero, tokens;
		byte line[];
		int i, j;

		ficheroSalida = new String[2];

		fichero = Fichero.leeFichero (ficheroScript);
		lineasFichero = new StringTokenizer (fichero,"\n\r");

		lineasFichero.nextToken();
		linea = lineasFichero.nextToken();

		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		token = tokens.nextToken();

		/*Getting the name of the training and test files*/
		line = token.getBytes();
		for (i=0; line[i]!='\"'; i++);
		i++;
		for (j=i; line[j]!='\"'; j++);
		ficheroTraining = new String (line,i,j-i);
		
		for (i=j+1; line[i]!='\"'; i++);
		i++;
		for (j=i; line[j]!='\"'; j++);
		ficheroValidation = new String (line,i,j-i);
		
		for (i=j+1; line[i]!='\"'; i++);
		i++;
		for (j=i; line[j]!='\"'; j++);
		ficheroTest = new String (line,i,j-i);

		/*Getting the path and base name of the results files*/
		linea = lineasFichero.nextToken();
		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		token = tokens.nextToken();

		/*Getting the names of the output files*/
		line = token.getBytes();
		for (i=0; line[i]!='\"'; i++);
		i++;
		for (j=i; line[j]!='\"'; j++);
		ficheroSalida[0] = new String (line,i,j-i);
		for (i=j+1; line[i]!='\"'; i++);
		i++;
		for (j=i; line[j]!='\"'; j++);
		ficheroSalida[1] = new String (line,i,j-i);

		/*Getting the seed*/
		linea = lineasFichero.nextToken();
		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		semilla = Long.parseLong(tokens.nextToken().substring(1));

		/*Getting the mutation and cross probability*/
		linea = lineasFichero.nextToken();
		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		pMutacion1to0 = Double.parseDouble(tokens.nextToken().substring(1));
		linea = lineasFichero.nextToken();
		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		pMutacion0to1 = Double.parseDouble(tokens.nextToken().substring(1));
		linea = lineasFichero.nextToken();
		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		pCruce = Double.parseDouble(tokens.nextToken().substring(1));

		/*Getting the size of the population and number of evaluations*/
		linea = lineasFichero.nextToken();
		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		tamPoblacion = Integer.parseInt(tokens.nextToken().substring(1));
		linea = lineasFichero.nextToken();
		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		nEval = Integer.parseInt(tokens.nextToken().substring(1));

		/*Getting the alfa equilibrate factor*/
		linea = lineasFichero.nextToken();
		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		alfa = Double.parseDouble(tokens.nextToken().substring(1));

		/*Getting the type of selection*/
		linea = lineasFichero.nextToken();
		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		token = tokens.nextToken();
		token = token.substring(1);
		if (token.equalsIgnoreCase("binary_tournament")) torneo = true;
		else torneo = false;

		linea = lineasFichero.nextToken();
		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		kNeigh = Integer.parseInt(tokens.nextToken().substring(1));

		/*Getting the type of distance function*/
		linea = lineasFichero.nextToken();
		tokens = new StringTokenizer (linea, "=");
		tokens.nextToken();
		distanceEu = tokens.nextToken().substring(1).equalsIgnoreCase("Euclidean")?true:false;    
		
	}//end-method 
	
}//end-class
