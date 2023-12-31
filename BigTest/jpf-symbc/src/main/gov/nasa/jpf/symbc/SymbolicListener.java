/*
 * Copyright (C) 2014, United States Government, as represented by the
 * Administrator of the National Aeronautics and Space Administration.
 * All rights reserved.
 *
 * Symbolic Pathfinder (jpf-symbc) is licensed under the Apache License, 
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 *        http://www.apache.org/licenses/LICENSE-2.0. 
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and 
 * limitations under the License.
 */

package gov.nasa.jpf.symbc;

import gov.nasa.jpf.Config;
import gov.nasa.jpf.JPF;
import gov.nasa.jpf.PropertyListenerAdapter;
import gov.nasa.jpf.vm.ChoiceGenerator;
import gov.nasa.jpf.vm.ClassInfo;
import gov.nasa.jpf.vm.DoubleFieldInfo;
import gov.nasa.jpf.vm.ElementInfo;
import gov.nasa.jpf.vm.FieldInfo;
import gov.nasa.jpf.vm.Fields;
import gov.nasa.jpf.vm.FloatFieldInfo;
import gov.nasa.jpf.vm.Instruction;
import gov.nasa.jpf.vm.IntegerFieldInfo;
import gov.nasa.jpf.vm.LocalVarInfo;
import gov.nasa.jpf.vm.LongFieldInfo;
import gov.nasa.jpf.vm.MJIEnv;
import gov.nasa.jpf.vm.MethodInfo;
import gov.nasa.jpf.vm.ReferenceFieldInfo;
import gov.nasa.jpf.vm.StackFrame;
import gov.nasa.jpf.vm.ThreadInfo;
import gov.nasa.jpf.vm.Types;
import gov.nasa.jpf.vm.VM;
import symexScala.PathEffectListenerImp;
import gov.nasa.jpf.jvm.bytecode.ARETURN;
import gov.nasa.jpf.jvm.bytecode.DRETURN;
import gov.nasa.jpf.jvm.bytecode.FRETURN;
import gov.nasa.jpf.jvm.bytecode.IRETURN;
import gov.nasa.jpf.jvm.bytecode.JVMInvokeInstruction;
import gov.nasa.jpf.jvm.bytecode.LRETURN;
import gov.nasa.jpf.jvm.bytecode.JVMReturnInstruction;
import gov.nasa.jpf.report.ConsolePublisher;
import gov.nasa.jpf.report.Publisher;
import gov.nasa.jpf.report.PublisherExtension;
import gov.nasa.jpf.search.Search;
import gov.nasa.jpf.symbc.bytecode.BytecodeUtils;
import gov.nasa.jpf.symbc.bytecode.INVOKESTATIC;
import gov.nasa.jpf.symbc.concolic.PCAnalyzer;

import gov.nasa.jpf.symbc.numeric.Comparator;
import gov.nasa.jpf.symbc.numeric.Expression;
import gov.nasa.jpf.symbc.numeric.IntegerConstant;
import gov.nasa.jpf.symbc.numeric.IntegerExpression;
import gov.nasa.jpf.symbc.numeric.PCChoiceGenerator;
import gov.nasa.jpf.symbc.numeric.PathCondition;
import gov.nasa.jpf.symbc.numeric.RealConstant;
import gov.nasa.jpf.symbc.numeric.RealExpression;
import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.symbc.numeric.SymbolicReal;
import gov.nasa.jpf.symbc.string.StringComparator;
import gov.nasa.jpf.symbc.string.StringConstant;
import gov.nasa.jpf.symbc.string.StringExpression;
import gov.nasa.jpf.symbc.string.StringSymbolic;
import gov.nasa.jpf.symbc.numeric.SymbolicConstraintsGeneral;
//import gov.nasa.jpf.symbc.numeric.SymbolicInteger;
import gov.nasa.jpf.util.Pair;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.Vector;
public class SymbolicListener extends PropertyListenerAdapter implements PublisherExtension {
	/*
	 * Locals to preserve the value that was held by JPF prior to changing it in order to turn off state matching during
	 * symbolic execution no longer necessary because we run spf stateless
	 */
	private String currentMethodName = "";
	
	private int refDepth = -1;


	// probably we do not need this!
	private Map<Integer, SymbolicInteger> nameMap =
											new HashMap<Integer,SymbolicInteger>();

	// what are these fields?
	private Set<String> definedFields = new HashSet<String>();

	
	private PathEffectListener pathAndEffectL = null;
	public SymbolicListener(Config conf, JPF jpf) {
		jpf.addPublisherExtension(ConsolePublisher.class, this);
		pathAndEffectL = new PathEffectListenerImp();   
	}

	public SymbolicListener(Config conf, JPF jpf, PathEffectListener pe) {
		jpf.addPublisherExtension(ConsolePublisher.class, this);
		pathAndEffectL = pe;
	}

	static void println(Object o) {System.out.println("[Listener] " + o);}


	@Override
	public void propertyViolated(Search search) {

		VM vm = search.getVM();

		ChoiceGenerator<?> cg = vm.getChoiceGenerator();
		if (!(cg instanceof PCChoiceGenerator)) {
			ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
			while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
				prev_cg = prev_cg.getPreviousChoiceGenerator();
			}
			cg = prev_cg;
		}
		if ((cg instanceof PCChoiceGenerator) && ((PCChoiceGenerator) cg).getCurrentPC() != null) {
			PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
			String error = search.getLastError().getDetails();
			error = "\"" + error.substring(0, error.indexOf("\n")) + "...\"";
			// C: not clear where result was used here -- to review
			// PathCondition result = new PathCondition();
			// IntegerExpression sym_err = new SymbolicInteger("ERROR");
			// IntegerExpression sym_value = new SymbolicInteger(error);
			// result._addDet(Comparator.EQ, sym_err, sym_value);
			// solve the path condition, then print it
			// pc.solve();
			if (SymbolicInstructionFactory.concolicMode) { // TODO: cleaner
				SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
				PCAnalyzer pa = new PCAnalyzer();
				pa.solve(pc, solver);
			} else
				pc.solve();

			Pair<String, String> pcPair = new Pair<String, String>(pc.toString(), error);// (pc.toString(),error);

		}
	}

	@Override
	public void instructionExecuted(VM vm, ThreadInfo currentThread, Instruction nextInstruction,
			Instruction executedInstruction) {
		try {
			if (!vm.getSystemState().isIgnored()) {
				Instruction insn = executedInstruction;
				// SystemState ss = vm.getSystemState();
				
				ThreadInfo ti = currentThread;
				Config conf = vm.getConfig();

				if (insn instanceof JVMInvokeInstruction) {
					JVMInvokeInstruction md = (JVMInvokeInstruction) insn;
					String methodName = md.getInvokedMethodName();
					int numberOfArgs = md.getArgumentValues(ti).length;

					MethodInfo mi = md.getInvokedMethod();
					ClassInfo ci = mi.getClassInfo();
					String className = ci.getName();

					//                if(mi.getName().contains("apply")){
						//                    System.out.println("fffff");
					//                }


					StackFrame sf = ti.getTopFrame();
					String shortName = methodName;
					String longName = mi.getLongName();
					if (methodName.contains("("))
						shortName = methodName.substring(0, methodName.indexOf("("));

					if (!mi.equals(sf.getMethodInfo()))
						return;

					if ((BytecodeUtils.isClassSymbolic(conf, className, mi, methodName))
							|| BytecodeUtils.isMethodSymbolic(conf, mi.getFullName(), numberOfArgs, null)) {

						MethodSummary methodSummary = new MethodSummary();

						methodSummary.setMethodName(className + "." + shortName);
						Object[] argValues = md.getArgumentValues(ti);
						String argValuesStr = "";
						for (int i = 0; i < argValues.length; i++) {
							argValuesStr = argValuesStr + argValues[i];
							if ((i + 1) < argValues.length)
								argValuesStr = argValuesStr + ",";
						}
						methodSummary.setArgValues(argValuesStr);
						byte[] argTypes = mi.getArgumentTypes();
						String argTypesStr = "";
						for (int i = 0; i < argTypes.length; i++) {
							argTypesStr = argTypesStr + argTypes[i];
							if ((i + 1) < argTypes.length)
								argTypesStr = argTypesStr + ",";
						}
						methodSummary.setArgTypes(argTypesStr);

						// get the symbolic values (changed from constructing them here)
						String symValuesStr = "";
						String symVarNameStr = "";

						LocalVarInfo[] argsInfo = mi.getArgumentLocalVars();
						//for (LocalVarInfo x: argsInfo)
						//	println(x);

						if (argsInfo == null)
							throw new RuntimeException("ERROR: you need to turn debug option on");

						int sfIndex = 1; // do not consider implicit param "this"
						int namesIndex = 1;
						if (md instanceof INVOKESTATIC) {
							//println("Static method");
							sfIndex = 0; // no "this" for static
							namesIndex = 0;
						}
						                    //if (md instanceof  gov.nasa.jpf.jvm.bytecode.INVOKESTATIC) {
						                    //    println("Static method");
						                    //    sfIndex = 0; // no "this" for static
						                    //    namesIndex = 0;
						                    //}

						for (int i = 0; i < numberOfArgs; i++) {
							// println(insn);
							// System.out.println(insn.getClass());
							// System.out.println(insn instanceof gov.nasa.jpf.symbc.bytecode.INVOKESTATIC);
							// System.out.println(insn instanceof gov.nasa.jpf.jvm.bytecode.INVOKESTATIC);
							// System.out.println(gov.nasa.jpf.jvm.bytecode.INVOKESTATIC.class.isAssignableFrom(gov.nasa.jpf.symbc.bytecode.INVOKESTATIC.class));
							// println("index:" + i + " numberOfArgs:"+numberOfArgs + " namesIndex:"+namesIndex+" argsInfo.length:"+argsInfo.length);
							
							Expression expLocal = (Expression) sf.getLocalAttr(sfIndex);
							if (expLocal != null) // symbolic
								symVarNameStr = expLocal.toString();
							else
								symVarNameStr = argsInfo[namesIndex].getName() + "_CONCRETE" + ",";
							// TODO: what happens if the argument is an array?
							symValuesStr = symValuesStr + symVarNameStr + ",";
							sfIndex++;
							namesIndex++;
							if (argTypes[i] == Types.T_LONG || argTypes[i] == Types.T_DOUBLE)
								sfIndex++;

						}

						// get rid of last ","
						if (symValuesStr.endsWith(",")) {
							symValuesStr = symValuesStr.substring(0, symValuesStr.length() - 1);
						}
						methodSummary.setSymValues(symValuesStr);

						currentMethodName = longName;
						// allSummaries.put(longName, methodSummary);
					}
				} else if (insn instanceof JVMReturnInstruction) {
					MethodInfo mi = insn.getMethodInfo();
					ClassInfo ci = mi.getClassInfo();
					if (null != ci) {
						String className = ci.getName();
						String methodName = mi.getName();
						String longName = mi.getLongName();
						int numberOfArgs = mi.getNumberOfArguments();
						if (((BytecodeUtils.isClassSymbolic(conf, className, mi, methodName))
								|| BytecodeUtils.isMethodSymbolic(conf, mi.getFullName(), numberOfArgs, null))) {
					// System.out.println(insn);
							
							ChoiceGenerator<?> cg = vm.getChoiceGenerator();
							if (!(cg instanceof PCChoiceGenerator)) {
								ChoiceGenerator<?> prev_cg = cg.getPreviousChoiceGenerator();
								while (!((prev_cg == null) || (prev_cg instanceof PCChoiceGenerator))) {
									prev_cg = prev_cg.getPreviousChoiceGenerator();
								}
								cg = prev_cg;
							}
							if ((cg instanceof PCChoiceGenerator) && ((PCChoiceGenerator) cg).getCurrentPC() != null) {
								PathCondition pc = ((PCChoiceGenerator) cg).getCurrentPC();
								// pc.solve(); //we only solve the pc
								if (SymbolicInstructionFactory.concolicMode) { // TODO: cleaner
									SymbolicConstraintsGeneral solver = new SymbolicConstraintsGeneral();
									PCAnalyzer pa = new PCAnalyzer();
									pa.solve(pc, solver);
								} else
									pc.solve();

								if (!PathCondition.flagSolved) {
									return;
								}

								// after the following statement is executed, the pc loses its solution

								Pair<String, String> pcPair = null;
								//PathCondition resultp = new PathCondition();
								ArrayList<Expression> resultp = new ArrayList<Expression> ();
								
								//getFieldValues(resultp,ti,mi, insn);
								String returnString = "";

								Expression result = null;

								if (insn instanceof IRETURN) {
									IRETURN ireturn = (IRETURN) insn;
									int returnValue = ireturn.getReturnValue();
									IntegerExpression returnAttr = (IntegerExpression) ireturn.getReturnAttr(ti);
									if (returnAttr != null) {
										returnString = "Return Value: " + String.valueOf(returnAttr/*.solution()*/);
										result = returnAttr;
									} else { // concrete
										returnString = "Return Value: " + String.valueOf(returnValue);
										result = new IntegerConstant(returnValue);
									}
								} else if (insn instanceof LRETURN) {
									LRETURN lreturn = (LRETURN) insn;
									long returnValue = lreturn.getReturnValue();
									IntegerExpression returnAttr = (IntegerExpression) lreturn.getReturnAttr(ti);
									if (returnAttr != null) {
										returnString = "Return Value: " + String.valueOf(returnAttr.solution());
										result = returnAttr;
									} else { // concrete
										returnString = "Return Value: " + String.valueOf(returnValue);
										result = new IntegerConstant((int) returnValue);
									}
								} else if (insn instanceof DRETURN) {
									DRETURN dreturn = (DRETURN) insn;
									double returnValue = dreturn.getReturnValue();
									RealExpression returnAttr = (RealExpression) dreturn.getReturnAttr(ti);
									if (returnAttr != null) {
										returnString = "Return Value: " + String.valueOf(returnAttr.solution());
										result = returnAttr;
									} else { // concrete
										returnString = "Return Value: " + String.valueOf(returnValue);
										result = new RealConstant(returnValue);
									}
								} else if (insn instanceof FRETURN) {

									FRETURN freturn = (FRETURN) insn;
									double returnValue = freturn.getReturnValue();
									RealExpression returnAttr = (RealExpression) freturn.getReturnAttr(ti);
									if (returnAttr != null) {
										returnString = "Return Value: " + String.valueOf(returnAttr.solution());
										result = returnAttr;
									} else { // concrete
										returnString = "Return Value: " + String.valueOf(returnValue);
										result = new RealConstant(returnValue);
									}

								} else if (insn instanceof ARETURN) {
									ARETURN areturn = (ARETURN) insn;
									Object o = areturn.getReturnAttr(ti);
									if (o == null){
										getFieldValues(resultp,ti,mi, insn);
									}else if(o instanceof StringSymbolic ) {
										StringSymbolic returnAttr  = (StringSymbolic) o;
										returnString = "Return Value: " + String.valueOf(returnAttr.solution());
										result = returnAttr;
									}else if(o instanceof StringExpression){
										StringExpression returnAttr  = (StringExpression) o;
										returnString = "Return Value: " + String.valueOf(returnAttr);//.solution());
										result = returnAttr;
									} else {
										// @thaddywu: Need to be read
										IntegerExpression returnAttr = (IntegerExpression) o;
										if (returnAttr != null) {
											returnString = "Return Value: " + String.valueOf(returnAttr.solution());
											result = returnAttr;
										} else {// concrete, @thaddywu: dead code
											Object val = areturn.getReturnValue(ti);
											returnString = "Return Value: " + String.valueOf(val);
											// DynamicElementInfo val = (DynamicElementInfo)areturn.getReturnValue(ti);
											String tmp = String.valueOf(val);
											tmp = tmp.substring(tmp.lastIndexOf('.') + 1);
											result = new SymbolicInteger(tmp);
										}
									}
								} else // other types of return
								{ 
									returnString = "Return Value: --"; // Gulzar : We are not supporting the void methods
									// result = Void
								}// pc.solve();
								// not clear why this part is necessary
								/*
								 * if (SymbolicInstructionFactory.concolicMode) { //TODO: cleaner SymbolicConstraintsGeneral
								 * solver = new SymbolicConstraintsGeneral(); PCAnalyzer pa = new PCAnalyzer();
								 * pa.solve(pc,solver); } else pc.solve();
								 */

								// @thaddywu: need to be read
								LocalVarInfo[] argsInfo = mi.getArgumentLocalVars();
								//System.out.println(argsInfo.length+" -------- "+numberOfArgs);
								if(argsInfo.length >= numberOfArgs && !pathAndEffectL.isArgsInfoAdded()) {
									for(int i=argsInfo.length-numberOfArgs; i < argsInfo.length; ++i){
										String type = argsInfo[i].getType();
										println("\u001B[31m" + "type: " + type + "\u001B[0m"); // @thaddywu
										if(type.startsWith("Tuple") && type.length() == 7) {
											String t1 = type.charAt(5) == 'S' ? "java.lang.String" : "int";
											String t2 = type.charAt(6) == 'S' ? "java.lang.String" : "int";
											pathAndEffectL.addArgsInfo(argsInfo[i].getName()+"_1", t1);
											pathAndEffectL.addArgsInfo(argsInfo[i].getName()+"_2", t2);
										}else if(type.startsWith("Tuple") && type.length() == 8) {
											String t1 = type.charAt(5) == 'S' ? "java.lang.String" : "int";
											String t2 = type.charAt(6) == 'S' ? "java.lang.String" : "int";
											String t3 = type.charAt(7) == 'S' ? "java.lang.String" : "int";
											pathAndEffectL.addArgsInfo(argsInfo[i].getName()+"_1", t1);
											pathAndEffectL.addArgsInfo(argsInfo[i].getName()+"_2_1", t2);
											pathAndEffectL.addArgsInfo(argsInfo[i].getName()+"_2_2", t3);
										}
										else {
											pathAndEffectL.addArgsInfo(argsInfo[i].getName(), argsInfo[i].getType());
										}
									}
									pathAndEffectL.argsInfoIsAdded();
								}


								if(result != null && resultp.size() == 0) {
									resultp.add(result);
									// System.out.println(pc.toString()+" --B- "+result.toString()); 
								}
								//ArrayList<Expression> a = new ArrayList<>();
								//a.add(result);
								
								if(resultp.size() >0)// || (pc.header != null || pc.spc.header != null))
								{
									//if(resultp.get(0) instanceof IntegerConstant) {
									//	if( ((IntegerConstant)resultp.get(0)).value == 0 ) {
											//return;
									//	}else
										//	pathAndEffectL.addPCPair(pc, resultp); 									
												
									
									//}else
									pathAndEffectL.addPCPair(pc, resultp); 									
								}	
							}
						}
					}
				}else {
					
					//System.out.println(insn);
				}
			}
		}catch(Exception e) {
			e.printStackTrace();
		}
	}

		/*
		 * Recursive method to "dereference" an object and collect their values
		 * for use in the effects/result constraint
		 */

	Set<Integer> seenSet;
	int currentDepth=0;


	 void expandReferenceObject(ArrayList<Expression> e_list,ThreadInfo ti,
										ClassInfo ci,  int objNum){

		if ((currentDepth<=refDepth || refDepth == -1) &&
					!seenSet.contains(new Integer(objNum))){
			seenSet.add(new Integer(objNum));
			currentDepth++;
			String name = "";
			FieldInfo[] fields = ci.getDeclaredInstanceFields();
			ElementInfo ei = ti.getElementInfo(objNum);
			Integer ref = new Integer(objNum);
			
			// System.out.println("\u001B[31m ElementInfo: " + ei + " " + objNum + "\u001B[0m");
			// System.out.print("Fields:" );
			// for (int i = 0; i < fields.length; i++)
			// 	System.out.print(fields[i] + ", ");
			// System.out.println();

			if (ei != null && ei.isStringObject()) { // String
				// @thaddywu: fix StringConstant
				System.out.println(ei.getClass());
				System.out.println(ei.asString());
				e_list.add(new StringConstant(ei.asString()));
				return; 
			}

			if (null != ei && fields.length >0){
				for (int i = 0; i < fields.length; i++) {
					if (!fields[i].getName().contains("this")){
						SymbolicInteger temp = nameMap.get(ref);
						String fullType = fields[i].getType();
						String type = "";
						// C: why is this done???
					    if (fullType.contains("$"))
						  type = fullType.substring(fullType.indexOf('$')+1);
					    else
						  type = fullType.substring(fullType.lastIndexOf('.')+1);
						if (null != temp)
							name = nameMap.get(ref) + "." + type + ":" + fields[i].getName();
						else{ //this case is still not quite right
							name = ci.getName();
						    name = name.substring(name.lastIndexOf('.')+1) + ":#" + objNum + "." + fields[i].getName();
						}
						//System.out.println(name);
						if (!definedFields.contains(name)){
							definedFields.add(name);
							Object attr = ei.getFieldAttr(fields[i]);
							if (fields[i] instanceof IntegerFieldInfo ||
														fields[i] instanceof LongFieldInfo) {
								IntegerExpression symField = new SymbolicInteger(name);
								if (null != attr)
									//pc._addDet(Comparator.EQ, symField, (IntegerExpression)attr);
									e_list.add((IntegerExpression)attr);
								else{
									int val;
									if (fields[i] instanceof IntegerFieldInfo)
										val = ei.getFields().getIntValue(i);
									else  //WARNING: downcasting to an int
										val = (int)ei.getFields().getLongValue(i);
								//	pc._addDet(Comparator.EQ, symField, new IntegerConstant(val));
									if(val!=0)
									e_list.add(new IntegerConstant(val));
									
								}
							} else if (fields[i] instanceof FloatFieldInfo ||
										fields[i] instanceof DoubleFieldInfo) {
								RealExpression symField = new SymbolicReal(name);
								if (null != attr)
								//	pc._addDet(Comparator.EQ, symField, (RealExpression)attr);
								e_list.add( (RealExpression)attr);
								else{
									double val;
									if (fields[i] instanceof FloatFieldInfo)
										val = ei.getFields().getFloatValue(i);
									else
										val = ei.getFields().getDoubleValue(i);
									//pc._addDet(Comparator.EQ, symField, new RealConstant(val));
									e_list.add( new RealConstant( val));
								}
							}else if (fields[i] instanceof ReferenceFieldInfo){
								IntegerExpression symField= new SymbolicInteger(name);
								Fields f = ei.getFields();
								Object val = f.getFieldAttr(i);
								if (val != null)
									println("ReferenceFieldInfo val: " + val);
								int objIndex = f.getReferenceValue(i);
								// println("ReferenceFieldInfo ReferenceValue: " + (new Integer(objIndex)).toString());
								if (null == val){
									IntegerExpression exp = null;
									if (objIndex == MJIEnv.NULL){
										exp = new IntegerConstant(objIndex);
									//	pc._addDet(Comparator.EQ, symField, exp);
										//e_list.add( exp);
									}else{
										exp = nameMap.get(new Integer(objIndex));
										if (null == exp)
											exp = new IntegerConstant(objIndex);
									//	pc._addDet(Comparator.EQ, symField, exp);
										// e_list.add(exp); @thaddywu: commented
										if (objIndex != objNum && !seenSet.contains(objIndex) && objIndex != MJIEnv.NULL) 
										// @thaddywu: align with https://github.com/SymbolicPathFinder/jpf-symbc/blob/c98bfae2969e51923006e4c922c0f4e4a1cfbdfb/src/main/gov/nasa/jpf/symbc/heap/HeapSymbolicListener.java#L244
											// expandReferenceObject(e_list,ti,ci,objIndex);
											expandReferenceObject(e_list,ti,ti.getClassInfo(objIndex),objIndex); // @thaddywu: shouldn't pass ci as the classInfo, should get ClassInfo for objIndex
									}
								}else{
									//pc._addDet(Comparator.EQ, symField, new IntegerConstant(objIndex));
									if(val instanceof StringExpression) {
										//pc.spc._addDet(StringComparator.EQ, new StringSymbolic(name), (StringExpression) val);
										e_list.add((StringExpression) val);
									}else {
										//pc._addDet(Comparator.EQ, symField, (IntegerExpression) val);
										assert(false); // @thaddywu: I don't understand this case, leave as unfixed
										e_list.add((IntegerExpression) val);
										if (objIndex != objNum && !seenSet.contains(objIndex) && objIndex != MJIEnv.NULL)
											expandReferenceObject(e_list,ti,ci,objIndex);
									}
								}
							}
						}
					}
				}
			}

		}
	}

	/*
	 * Add the values (symbolic or concrete) of instance and static fields to the
	 * effects/result
	 * use refDepth configuration value to determine how far to "unwind" -- why is this necessary?
	 * object references
	 */
	private void getFieldValues(ArrayList<Expression> e_list, ThreadInfo ti,
										MethodInfo mi, Instruction insn){
		ClassInfo ci = mi.getClassInfo();
		JVMReturnInstruction ret = (JVMReturnInstruction)insn;
		StackFrame sf = ret.getReturnFrame();
		int thisRef = sf.getThis();

		// C: why is this string manipulation necessary?
		String name = sf.getClassName() + ":#" + thisRef;
		  if (name.contains("$"))
			  name = name.substring(name.indexOf('$')+1);
		  else
			  name = name.substring(name.lastIndexOf('.')+1);
		  String tmpName = name.substring(0,name.lastIndexOf('#')-1) + ":this";
		//  returnPC._addDet(Comparator.EQ, new SymbolicInteger(tmpName),
			//	  new SymbolicInteger(name));
		seenSet = new HashSet<Integer>();
		definedFields = new HashSet<String>();

		nameMap.put(new Integer(thisRef), new SymbolicInteger(name)); // why is this necessary

		// adds constraints representing this

		expandReferenceObject(e_list, ti, ci, thisRef);
		if (insn instanceof ARETURN){
			ARETURN areturn = (ARETURN)insn;
			int returnValue = areturn.getReturnValue();
			if (returnValue != thisRef)
				// adds constraints representing the return values
				expandReferenceObject(e_list, ti, ci, returnValue);
		}
	}
	protected class MethodSummary {
		private String methodName = "";
		private String argTypes = "";
		private String argValues = "";
		private String symValues = "";
		private Vector<Pair> pathConditions;

		public MethodSummary() {
			pathConditions = new Vector<Pair>();
		}

		public void setMethodName(String mName) {
			this.methodName = mName;
		}

		public String getMethodName() {
			return this.methodName;
		}

		public void setArgTypes(String args) {
			this.argTypes = args;
		}

		public String getArgTypes() {
			return this.argTypes;
		}

		public void setArgValues(String vals) {
			this.argValues = vals;
		}

		public String getArgValues() {
			return this.argValues;
		}

		public void setSymValues(String sym) {
			this.symValues = sym;
		}

		public String getSymValues() {
			return this.symValues;
		}

		public void addPathCondition(Pair pc) {
			pathConditions.add(pc);
		}

		public Vector<Pair> getPathConditions() {
			return this.pathConditions;
		}

	}
}
