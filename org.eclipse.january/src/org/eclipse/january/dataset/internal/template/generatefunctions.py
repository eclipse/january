#!/usr/bin/env python3
###
# *******************************************************************************
# * Copyright (c) 2011-2022 Diamond Light Source Ltd.
# * All rights reserved. This program and the accompanying materials
# * are made available under the terms of the Eclipse Public License v1.0
# * which accompanies this distribution, and is available at
# * http://www.eclipse.org/legal/epl-v10.html
# *
# * Contributors:
# *    Peter Chang - initial API and implementation and/or initial documentation
# *******************************************************************************/
###

'''
Takes a functions definition file and the shell of a Java class,
generates the methods and prints to standard output the completed class

Runs as follows
$ python3 generatefunctions.py functions.txt MathsPreface.java > Maths.java

The format is

func: [number of parameters]
  foo - javadoc for foo; @since 2.1
integer:
  ox = ix + 1
real:
  ox = ix + 1.5
complex:
  ox = ix + 1.2*iy
  oy = 2.3*iy - 0.5*ix

Any parameters are assumed to be numbers (double or Complex) and can be
referred to using pax, pbx, etc for real parts and pay, pby, etc for
imaginary parts. The order of the function specification should be set
as shown in the example. If integer code is not specified, then a case
is automatically generated with a promoted dataset type: int 8 & 16 to
float32 and int 32 & 64 to float64.

An "ifunc" allows integer datasets to be output.

Or a binary operation can be specified like:
biop: [number of parameters] [(s|u) [(s|u)]]
  add - a + b, addition of a and b; @since: 2.1
integer:
  ox = iax + ibx;
real:
  ox = iax + ibx;
complex:
  ox = iax + ibx;
  oy = iay + iby;

The optional letters after the number of parameters denote whether the
arguments are treated as signed or unsigned integers - they are "s" by
default. When "u" is specified, a (long) "unsignedMask" is defined
that will be available for use.

A 'complex_b_real;' marker can be appended to the 'complex' case to
handle the special case of the second operand being real
'''

#
# State machine code from David Mertz
# http://www.ibm.com/developerworks/library/l-python-state.html
#

class InitializationError(Exception): pass

class StateMachine:
    def __init__(self):
        self.handlers = []
        self.startState = None
        self.endStates = []

    def add_state(self, handler, end_state=0):
        self.handlers.append(handler)
        if end_state:
            self.endStates.append(handler)

    def set_start(self, handler):
        self.startState = handler

    def run(self, cargo=None):
        if not self.startState:
            raise InitializationError("must call .set_start() before .run()")
        if not self.endStates:
            raise InitializationError("at least one state must be an end_state")
        handler = self.startState
        while 1:
            (newState, cargo) = handler(cargo)
            if newState in self.endStates:
                newState(cargo)
                break
            elif newState not in self.handlers:
                raise RuntimeError("Invalid target %s" % newState)
            else:
                handler = newState

is_binaryop = False
def_unsigned_mask = False
allow_ints = False

def oldmethod(name, jdoc=None, edoc="", params=0):
    if is_binaryop:
        print("\t/**\n\t * %s operator" %  name)
        print("\t * @param a first operand")
        print("\t * @param b second operand")
    else:
        print("\t/**\n\t * %s - %s" %  (name, jdoc))
        print("\t * @param a single operand")

    if params > 0:
        cp = "pa"
        plist = [cp]
        jdlist = [cp + " %s parameter" % ORDINALS[0]]
        psig = "final Object " + cp
        for p in range(1, params):
            cp = "p"+chr(ord('a')+p)
            plist.append(cp)
            jdlist.append(cp + " %s parameter" % ORDINALS[p])
            psig += ", final Object " + cp

        for jd in jdlist:
            print("\t * @param %s" % jd)

        ptext = ", ".join(plist)
        if is_binaryop:
            print("\t * @return %s\n%s\t */" % (jdoc, edoc))
            print("\tpublic static Dataset %s(final Object a, final Object b, %s) {" % (name, psig))
            print("\t\treturn %s(a, b, null, %s);" % (name, ptext))
        else:
            print("\t * @return dataset\n%s\t */" % edoc)
            print("\tpublic static Dataset %s(final Object a, %s) {" % (name, psig))
            print("\t\treturn %s(a, null, %s);" % (name, ptext))
    else:
        if is_binaryop:
            print("\t * @return %s\n%s\t */" % (jdoc, edoc))
            print("\tpublic static Dataset %s(final Object a, final Object b) {" % name)
            print("\t\treturn %s(a, b, null);" % name)
        else:
            print("\t * @return dataset\n%s\t */" % edoc)
            print("\tpublic static Dataset %s(final Object a) {" % name)
            print("\t\treturn %s(a, null);" % name)
    print("\t}\n")

ORDINALS = ('first', 'second', 'third', 'fourth', 'fifth')
def beginmethod(name, jdoc=None, edoc=None, params=0):
    if edoc is None:
        edoc = ""
    else:
        edoc = "".join(["\t * %s\n" % e.strip() for e in edoc ])
    oldmethod(name, jdoc, edoc, params)
    if is_binaryop:
        print("\t/**\n\t * %s operator" %  name)
        print("\t * @param a first operand")
        print("\t * @param b second operand")
        print("\t * @param o output can be null - in which case, a new dataset is created")
    else:
        print("\t/**\n\t * %s - %s" %  (name, jdoc))
        print("\t * @param a single operand")
        print("\t * @param o output can be null - in which case, a new dataset is created")

    plist = []
    if params > 0:
        cp = "pa"
        plist.append(cp)
        jdlist = [cp + " %s parameter" % ORDINALS[0]]
        psig = "final Object " + cp
        for p in range(1, params):
            cp = "p"+chr(ord('a')+p)
            plist.append(cp)
            jdlist.append(cp + " %s parameter" % ORDINALS[p])
            psig += ", final Object " + cp

        for jd in jdlist:
            print("\t * @param %s" % jd)

        if is_binaryop:
            print("\t * @return %s\n%s\t */" % (jdoc, edoc))
            print("\tpublic static Dataset %s(final Object a, final Object b, final Dataset o, %s) {" % (name, psig))
        else:
            print("\t * @return dataset\n%s\t */" % edoc)
            print("\tpublic static Dataset %s(final Object a, final Dataset o, %s) {" % (name, psig))
    else:
        if is_binaryop:
            print("\t * @return %s\n%s\t */" % (jdoc, edoc))
            print("\tpublic static Dataset %s(final Object a, final Object b, final Dataset o) {" % name)
        else:
            print("\t * @return dataset\n%s\t */" % edoc)
            print("\tpublic static Dataset %s(final Object a, final Dataset o) {" % name)
    print("\t\tDataset da = a instanceof Dataset ? (Dataset) a : DatasetFactory.createFromObject(a);")
    if is_binaryop:
        print("\t\tDataset db = b instanceof Dataset ? (Dataset) b : DatasetFactory.createFromObject(b);")
        print("\t\tBroadcastIterator it = BroadcastIterator.createIterator(da, db, o, true);")
        if allow_ints:
            print("\t\tit.setOutputDouble(false);");
    else:
        if allow_ints:
            print("\t\tSingleInputBroadcastIterator it = new SingleInputBroadcastIterator(da, o, true, true, true);")
        else:
            print("\t\tSingleInputBroadcastIterator it = new SingleInputBroadcastIterator(da, o, true);")

    if def_unsigned_mask:
        print("\t\tfinal long unsignedMask;")


    print("\t\tfinal Dataset result = it.getOutput();")
    print("\t\tif (!result.isComplex()) {")
    if is_binaryop:
        print("\t\t\tboolean change = false;")
    print("\t\t\tif (da.isComplex()) {")
    print("\t\t\t\tda = da.getRealView();")
    if is_binaryop:
        print("\t\t\t\tchange = true;")
    else:
        if allow_ints:
            print("\t\t\t\tit = new SingleInputBroadcastIterator(da, result, true, true, true);")
        else:
            print("\t\t\t\tit = new SingleInputBroadcastIterator(da, result, true);")
    print("\t\t\t}")
    if is_binaryop:
        print("\t\t\tif (db.isComplex()) {")
        print("\t\t\t\tdb = db.getRealView();")
        print("\t\t\t\tchange = true;")
        print("\t\t\t}")
        print("\t\t\tif (change) {")
        print("\t\t\t\tit = BroadcastIterator.createIterator(da, db, result, true);")
        if allow_ints:
            print("\t\t\t\tit.setOutputDouble(false);");
        print("\t\t\t}")
    print("\t\t}")
    print("\t\tfinal int is = result.getElementsPerItem();")
    print("\t\tfinal int as = da.getElementsPerItem();")
    if is_binaryop:
        print("\t\tfinal int bs = db.getElementsPerItem();")
    print("\t\tfinal int dt = result.getDType();")
    for p in plist:
        print("\t\tfinal double %s = DTypeUtils.toReal(%s);" % (p+"x", p))
#        print("\t\tfinal double %s = DTypeUtils.toImag(%s);" % (p+"y", p))

    print("")
    print("\t\tswitch(dt) {")


def endmethod(name, jdoc, types):
    print("\t\tdefault:")
    dtypes = types[0]
    for t in types[1:]:
        dtypes += ", %s" % t
    print("\t\t\tthrow new IllegalArgumentException(\"%s supports %s datasets only\");" % (name, dtypes))
    print("\t\t}\n")
    if is_binaryop:
        opsym = jdoc.split()[1]
        print("\t\taddBinaryOperatorName(da, db, result, \"%s\");" % opsym)
    else:
        print("\t\taddFunctionName(result, \"%s\");" % name)
    print("\t\treturn result;")
    print("\t}\n")

def sameloop(codedict, cprefix, vletter, text, use_long=False, override_long=False, unsigned=False):
    is_int = cprefix.endswith("INT")
    for w in codedict.keys():
        dtype = "%s%d" % (cprefix, w)
        otype, oclass = codedict[w]
        ovar = "o%s%ddata" % (vletter,w)
        if unsigned:
            mask = "0x" + (w // 4) * "f" + "L"
        else:
            mask = None
        preloop(dtype, otype, oclass, ovar, is_int, use_long, override_long=override_long, mask=mask)
        loop(text, otype, ovar, is_int, override_long)
        postloop()

def complexloop(codedict, cprefix, vletter, text, text_b_real, real):
    is_int = cprefix.endswith("INT")
    for w in codedict.keys():
        dtype = "%s%d" % (cprefix, w)
        ovar = "o%s%ddata" % (vletter,w)
        otype, oclass, owide = codedict[w]
        preloop(dtype, otype, oclass, ovar, is_int)
        loopcomplex(text, text_b_real, otype, ovar, real, is_int)
        postloop()

def compoundloop(codedict, cprefix, vletter, text, use_long=False, override_long=False, unsigned=False):
    is_int = cprefix.endswith("INT")
    for w in codedict.keys():
        dtype = "%s%d" % (cprefix, w)
        ovar = "o%s%ddata" % (vletter,w)
        otype, oclass = codedict[w]
        if unsigned:
            mask = "0x" + (w // 4) * "f" + "L"
        else:
            mask = None
        preloop(dtype, otype, oclass, ovar, is_int, use_long, override_long=override_long, mask=mask)
        loopcompound(text, otype, ovar, is_int, override_long)
        postloop()

def deftemps(text, jtype, lprefix, vars):
#    vars = { 'ox':jtype, 'oy':jtype }
    if vars is None:
        vars = {}
    for t in text:
        # need to build up list of temporaries
        if t.find(" = ") >= 0:
            lhs,rhs = t.split(" = ",1)
            lhs = lhs.strip()
            if rhs != None and lhs not in vars:
                # check for object instantiation then declare new variable
                if t.find(" new") >= 0:
                    dummy, rest = t.split(" new ", 1)
                    lclass,dummy = rest.split("(",1)
                    if lclass != "":
                        vars[lhs] = lclass
                        print("%s%s %s;" % (lprefix, lclass, lhs))
                    else:
                        raise ValueError("Cannot find class of new variable in line: %s" % t)
                else:
                    vars[lhs] = jtype
                    print("%s%s %s;" % (lprefix, jtype, lhs))
    return vars

import re
param_re = re.compile("p[a-z][xy]")

def transtext(text, jtype, otype=None, lprefix="\t\t\t\t\t", is_int=True, override_long=False, use_long=False, vars=None):
    if otype == None:
        otype = jtype
    vars = deftemps(text, jtype, lprefix, vars)

    is_real = (otype == "float" and not is_int) or otype == "double"

    if is_int:
        jprim = "long"
        if is_binaryop and not override_long:
            jconv = ""
        else:
            jconv = "toLong"
    else:
        jprim = "double"
#        if otype == "long" or is_real:
        if is_real:
            jconv = ""
        else:
            jconv = "toLong"


    for t in text:
        # need to build up list of temporaries
        if t.find(" = ") >= 0:
            lhs, rhs = t.split(" = ", 1)
            slhs = lhs.strip()
            if slhs not in vars:
                raise ValueError("Cannot find class of new variable in line: %s" % t)
            if t.find(" new") < 0:
                rhs = rhs[:-2]
                if rhs == "0":
                    print("%s%s = 0;" % (lprefix, lhs))
                elif vars[slhs] == jprim:
                    print("%s%s = %s(%s);" % (lprefix, lhs, jconv, rhs))
                elif is_real:
#                     print("%s%s = (%s) (%s);" % (lprefix, lhs, vars[slhs], rhs)
                    if use_long and rhs.find("Math.") < 0 and param_re.search(rhs) is None:
                        print("%s%s = (%s);" % (lprefix, lhs, rhs))
                    else:
                        print("%s%s = (%s) (%s);" % (lprefix, lhs, vars[slhs], rhs))
                else:
                    if not is_real and otype == "long":
                        print("%s%s = %s(%s);" % (lprefix, lhs, jconv, rhs))
                    else:
                        print("%s%s = (%s) %s(%s);" % (lprefix, lhs, vars[slhs], jconv, rhs))
            else:
                print("%s%s" % (lprefix, t), end='')
        else:
            print("%s%s" % (lprefix, t), end='')

    return vars

def loop(text, jtype, ovar, is_int, override_long):
    if not allow_ints:
        print("\t\t\tif (it.isOutputDouble()) {")
        print("\t\t\t\twhile (it.hasNext()) {")
        if is_binaryop:
            print("\t\t\t\t\tfinal double iax = it.aDouble;")
            print("\t\t\t\t\tfinal double ibx = it.bDouble;")
        else:
            print("\t\t\t\t\tfinal double ix = it.aDouble;")
        transtext(text, jtype, is_int=False, override_long=override_long)
        print("\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
        print("\t\t\t\t}")
        print("\t\t\t} else {")
    else:
        print("\t\t\t{")
    print("\t\t\t\twhile (it.hasNext()) {")
    if is_binaryop:
        print("\t\t\t\t\tfinal long iax = it.aLong;")
        print("\t\t\t\t\tfinal long ibx = it.bLong;")
    else:
        print("\t\t\t\t\tfinal long ix = it.aLong;")
    transtext(text, jtype, is_int=is_int, override_long=override_long, use_long=True)
    print("\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
    print("\t\t\t\t}")
    print("\t\t\t}")

def loopcomplex(text, text_b_real, jtype, ovar, real, is_int):
    print("\t\t\tif (!da.isComplex()) {")
    print("\t\t\t\tif (it.isOutputDouble()) {")
    if is_binaryop:
        print("\t\t\t\t\tfinal double iay = 0;")
        print("\t\t\t\t\tif (db.isComplex()) {")
        print("\t\t\t\t\t\twhile (it.hasNext()) {")
        print("\t\t\t\t\t\t\tfinal double iax = it.aDouble;")
        print("\t\t\t\t\t\t\tfinal double ibx = it.bDouble;")
        print("\t\t\t\t\t\t\tfinal double iby = db.getElementDoubleAbs(it.bIndex + 1);")
        transtext(text, jtype, lprefix="\t\t\t\t\t\t\t", is_int=is_int)
        print("\t\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
        if not real:
            print("\t\t\t\t\t\t\t%s[it.oIndex + 1] = oy;" % ovar)
        print("\t\t\t\t\t\t}")
        print("\t\t\t\t\t} else {")
        print("\t\t\t\t\t\twhile (it.hasNext()) {")
        print("\t\t\t\t\t\t\tfinal double iax = it.aDouble;")
        print("\t\t\t\t\t\t\tfinal double ibx = it.bDouble;")
        if text_b_real:
            transtext(text_b_real, jtype, lprefix="\t\t\t\t\t\t\t", is_int=is_int)
        else:
            print("\t\t\t\t\t\t\tfinal double iby = 0;")
            transtext(text, jtype, lprefix="\t\t\t\t\t\t\t", is_int=is_int)
        print("\t\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
        if not real:
            print("\t\t\t\t\t\t\t%s[it.oIndex + 1] = oy;" % ovar)
        print("\t\t\t\t\t\t}")
        print("\t\t\t\t\t}")
    else:
        print("\t\t\t\t\tfinal double iy = 0;")
    
        print("\t\t\t\t\twhile (it.hasNext()) {")
        print("\t\t\t\t\t\tfinal double ix = it.aDouble;")
        transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=is_int)
        print("\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
        if not real:
            print("\t\t\t\t\t\t%s[it.oIndex + 1] = oy;" % ovar)
        print("\t\t\t\t\t}")

    print("\t\t\t\t} else {")
    if is_binaryop:
        print("\t\t\t\t\tfinal long iay = 0;")
    else:
        print("\t\t\t\t\tfinal long iy = 0;")
    print("\t\t\t\t\twhile (it.hasNext()) {")
    if is_binaryop:
        print("\t\t\t\t\t\tfinal long iax = it.aLong;")
        print("\t\t\t\t\t\tfinal long ibx = it.bLong;")
        if not text_b_real:
            print("\t\t\t\t\t\tfinal long iby = 0;")
    else:
        print("\t\t\t\t\t\tfinal long ix = it.aLong;")
    if is_binaryop and text_b_real:
        transtext(text_b_real, jtype, lprefix="\t\t\t\t\t\t", is_int=True, use_long=True)
    else:
        transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=True, use_long=True)
    print("\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
    if not real:
        print("\t\t\t\t\t\t%s[it.oIndex + 1] = oy;" % ovar)
    print("\t\t\t\t\t}")
    print("\t\t\t\t}")
    if is_binaryop:
        print("\t\t\t} else if (!db.isComplex()) {")
        if not text_b_real:
            print("\t\t\t\tfinal double iby = 0;")
        print("\t\t\t\twhile (it.hasNext()) {")
        print("\t\t\t\t\tfinal double iax = it.aDouble;")
        print("\t\t\t\t\tfinal double ibx = it.bDouble;")
        print("\t\t\t\t\tfinal double iay = da.getElementDoubleAbs(it.aIndex + 1);")
        if text_b_real:
            transtext(text_b_real, jtype, lprefix="\t\t\t\t\t", is_int=is_int)
        else:
            transtext(text, jtype, lprefix="\t\t\t\t\t", is_int=is_int)
        print("\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
        if not real:
            print("\t\t\t\t\t%s[it.oIndex + 1] = oy;" % ovar)
        print("\t\t\t\t}")
    print("\t\t\t} else {")
    print("\t\t\t\twhile (it.hasNext()) {")
    if is_binaryop:
        print("\t\t\t\t\tfinal double iax = it.aDouble;")
        print("\t\t\t\t\tfinal double ibx = it.bDouble;")
        print("\t\t\t\t\tfinal double iay = da.getElementDoubleAbs(it.aIndex + 1);")
        print("\t\t\t\t\tfinal double iby = db.getElementDoubleAbs(it.bIndex + 1);")
    else:
        print("\t\t\t\t\tfinal double ix = it.aDouble;")
        print("\t\t\t\t\tfinal double iy = da.getElementDoubleAbs(it.aIndex + 1);")
    transtext(text, jtype, lprefix="\t\t\t\t\t", is_int=is_int)
    print("\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
    if not real:
        print("\t\t\t\t\t%s[it.oIndex + 1] = oy;" % ovar)
    print("\t\t\t\t}")
    print("\t\t\t}")

def loopcompound(text, jtype, ovar, is_int, override_long):
    print("\t\t\tif (is == 1) {")
    if not allow_ints:
        print("\t\t\t\tif (it.isOutputDouble()) {")
        print("\t\t\t\t\twhile (it.hasNext()) {")
        if is_binaryop:
            print("\t\t\t\t\t\tfinal double iax = it.aDouble;")
            print("\t\t\t\t\t\tfinal double ibx = it.bDouble;")
        else:
            print("\t\t\t\t\t\tfinal double ix = it.aDouble;")
        transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=False, override_long=override_long)
        print("\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
        print("\t\t\t\t\t}")
        print("\t\t\t\t} else {")
    else:
        print("\t\t\t\t{")
    print("\t\t\t\t\twhile (it.hasNext()) {")
    if is_binaryop:
        print("\t\t\t\t\t\tfinal long iax = it.aLong;")
        print("\t\t\t\t\t\tfinal long ibx = it.bLong;")
    else:
        print("\t\t\t\t\t\tfinal long ix = it.aLong;")
    transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=is_int, override_long=override_long, use_long=True)
    print("\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
    print("\t\t\t\t\t}")
    print("\t\t\t\t}")

    if is_binaryop:
        print("\t\t\t} else if (as < bs) {")
        if not allow_ints:
            print("\t\t\t\tif (it.isOutputDouble()) {")
            print("\t\t\t\t\twhile (it.hasNext()) {")
            print("\t\t\t\t\t\tfinal double iax = it.aDouble;")
            print("\t\t\t\t\t\tdouble ibx = it.bDouble;")
            vars = transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=False, override_long=override_long)
            print("\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
            print("\t\t\t\t\t\tfor (int j = 1; j < is; j++) {")
            print("\t\t\t\t\t\t\tibx = db.getElementDoubleAbs(it.bIndex + j);")
            transtext(text, jtype, lprefix="\t\t\t\t\t\t\t", is_int=False, vars=vars, override_long=override_long)
            print("\t\t\t\t\t\t\t%s[it.oIndex + j] = ox;" % ovar)
            print("\t\t\t\t\t\t}")
            print("\t\t\t\t\t}")
            print("\t\t\t\t} else {")
        else:
            print("\t\t\t\t{")
        print("\t\t\t\t\twhile (it.hasNext()) {")
        print("\t\t\t\t\t\tfinal long iax = it.aLong;")
        print("\t\t\t\t\t\tlong ibx = it.bLong;")
        vars = transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=is_int, override_long=override_long, use_long=True)
        print("\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
        print("\t\t\t\t\t\tfor (int j = 1; j < is; j++) {")
        print("\t\t\t\t\t\t\tibx = db.getElementLongAbs(it.bIndex + j);")
        transtext(text, jtype, lprefix="\t\t\t\t\t\t\t", is_int=is_int, vars=vars, override_long=override_long, use_long=True)
        print("\t\t\t\t\t\t\t%s[it.oIndex + j] = ox;" % ovar)
        print("\t\t\t\t\t\t}")
        print("\t\t\t\t\t}")
        print("\t\t\t\t}")
    else:
        print("\t\t\t} else if (as == 1) {")
        if not allow_ints:
            print("\t\t\t\tif (it.isOutputDouble()) {")
            print("\t\t\t\t\twhile (it.hasNext()) {")
            print("\t\t\t\t\t\tfinal double ix = it.aDouble;")
            transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=False, override_long=override_long)
            print("\t\t\t\t\t\tfor (int j = 0; j < is; j++) {")
            print("\t\t\t\t\t\t\t%s[it.oIndex + j] = ox;" % ovar)
            print("\t\t\t\t\t\t}")
            print("\t\t\t\t\t}")
            print("\t\t\t\t} else {")
        else:
            print("\t\t\t\t{")
        print("\t\t\t\t\twhile (it.hasNext()) {")
        print("\t\t\t\t\t\tfinal long ix = it.aLong;")
        transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=is_int, override_long=override_long, use_long=True)
        print("\t\t\t\t\t\tfor (int j = 0; j < is; j++) {")
        print("\t\t\t\t\t\t\t%s[it.oIndex + j] = ox;" % ovar)
        print("\t\t\t\t\t\t}")
        print("\t\t\t\t\t}")
        print("\t\t\t\t}")

    if is_binaryop:
        print("\t\t\t} else if (as > bs) {")
        if not allow_ints:
            print("\t\t\t\tif (it.isOutputDouble()) {")
            print("\t\t\t\t\twhile (it.hasNext()) {")
            print("\t\t\t\t\t\tdouble iax = it.aDouble;")
            print("\t\t\t\t\t\tfinal double ibx = it.bDouble;")
            vars = transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=False, override_long=override_long)
            print("\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
            print("\t\t\t\t\t\tfor (int j = 1; j < is; j++) {")
            print("\t\t\t\t\t\t\tiax = da.getElementDoubleAbs(it.aIndex + j);")
            transtext(text, jtype, lprefix="\t\t\t\t\t\t\t", is_int=False, vars=vars, override_long=override_long)
            print("\t\t\t\t\t\t\t%s[it.oIndex + j] = ox;" % ovar)
            print("\t\t\t\t\t\t}")
            print("\t\t\t\t\t}")
            print("\t\t\t\t} else {")
        else:
            print("\t\t\t\t{")
        print("\t\t\t\t\twhile (it.hasNext()) {")
        print("\t\t\t\t\t\tlong iax = it.aLong;")
        print("\t\t\t\t\t\tfinal long ibx = it.bLong;")
        vars = transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=is_int, override_long=override_long, use_long=True)
        print("\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
        print("\t\t\t\t\t\tfor (int j = 1; j < is; j++) {")
        print("\t\t\t\t\t\t\tiax = da.getElementLongAbs(it.aIndex + j);")
        transtext(text, jtype, lprefix="\t\t\t\t\t\t\t", is_int=is_int, vars=vars, override_long=override_long, use_long=True)
        print("\t\t\t\t\t\t\t%s[it.oIndex + j] = ox;" % ovar)
        print("\t\t\t\t\t\t}")
        print("\t\t\t\t\t}")
        print("\t\t\t\t}")
        print("\t\t\t} else if (as == 1) {")
        if not allow_ints:
            print("\t\t\t\tif (it.isOutputDouble()) {")
            print("\t\t\t\t\twhile (it.hasNext()) {")
            print("\t\t\t\t\t\tfinal double iax = it.aDouble;")
            print("\t\t\t\t\t\tfinal double ibx = it.bDouble;")
            vars = transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=False, override_long=override_long)
            print("\t\t\t\t\t\tfor (int j = 0; j < is; j++) {")
            print("\t\t\t\t\t\t\t%s[it.oIndex + j] = ox;" % ovar)
            print("\t\t\t\t\t\t}")
            print("\t\t\t\t\t}")
            print("\t\t\t\t} else {")
        else:
            print("\t\t\t\t{")
        print("\t\t\t\t\twhile (it.hasNext()) {")
        print("\t\t\t\t\t\tfinal long iax = it.aLong;")
        print("\t\t\t\t\t\tfinal long ibx = it.bLong;")
        vars = transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=is_int, override_long=override_long, use_long=True)
        print("\t\t\t\t\t\tfor (int j = 0; j < is; j++) {")
        print("\t\t\t\t\t\t\t%s[it.oIndex + j] = ox;" % ovar)
        print("\t\t\t\t\t\t}")
        print("\t\t\t\t\t}")
        print("\t\t\t\t}")
    print("\t\t\t} else {")
    if not allow_ints:
        print("\t\t\t\tif (it.isOutputDouble()) {")
        print("\t\t\t\t\twhile (it.hasNext()) {")
        if is_binaryop:
            print("\t\t\t\t\t\tdouble iax = it.aDouble;")
            print("\t\t\t\t\t\tdouble ibx = it.bDouble;")
            vars = transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=False, override_long=override_long)
            print("\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
            print("\t\t\t\t\t\tfor (int j = 1; j < is; j++) {")
            print("\t\t\t\t\t\t\tiax = da.getElementDoubleAbs(it.aIndex + j);")
            print("\t\t\t\t\t\t\tibx = db.getElementDoubleAbs(it.bIndex + j);")
        else:
            vars = None
            print("\t\t\t\t\t\tfor (int j = 0; j < is; j++) {")
            print("\t\t\t\t\t\t\tfinal double ix = da.getElementDoubleAbs(it.aIndex + j);")
        transtext(text, jtype, lprefix="\t\t\t\t\t\t\t", is_int=False, vars=vars, override_long=override_long)
        print("\t\t\t\t\t\t\t%s[it.oIndex + j] = ox;" % ovar)
        print("\t\t\t\t\t\t}")
        print("\t\t\t\t\t}")
        print("\t\t\t\t} else {")
    else:
        print("\t\t\t\t{")
    print("\t\t\t\t\twhile (it.hasNext()) {")
    if is_binaryop:
        print("\t\t\t\t\t\tlong iax = it.aLong;")
        print("\t\t\t\t\t\tlong ibx = it.bLong;")
        vars = transtext(text, jtype, lprefix="\t\t\t\t\t\t", is_int=is_int, override_long=override_long, use_long=True)
        print("\t\t\t\t\t\t%s[it.oIndex] = ox;" % ovar)
        print("\t\t\t\t\t\tfor (int j = 1; j < is; j++) {")
        print("\t\t\t\t\t\t\tiax = da.getElementLongAbs(it.aIndex + j);")
        print("\t\t\t\t\t\t\tibx = db.getElementLongAbs(it.bIndex + j);")
    else:
        vars = None
        print("\t\t\t\t\t\tfor (int j = 0; j < is; j++) {")
        print("\t\t\t\t\t\t\tfinal long ix = da.getElementLongAbs(it.aIndex + j);")
    transtext(text, jtype, lprefix="\t\t\t\t\t\t\t", is_int=is_int, vars=vars, override_long=override_long, use_long=True)
    print("\t\t\t\t\t\t\t%s[it.oIndex + j] = ox;" % ovar)
    print("\t\t\t\t\t\t}")
    print("\t\t\t\t\t}")
    print("\t\t\t\t}")

    print("\t\t\t}")

def preloop(dtype, otype, oclass, ovar=None, is_int=True, use_long=False, override_long=False, mask=None):
    print("\t\tcase Dataset.%s:" % dtype)
    print("\t\t\tfinal %s[] %s = ((%s) result).getData();" % (otype, ovar, oclass))
    if is_binaryop or use_long:
        if is_int and not override_long:
            if mask is not None:
                print("\t\t\tunsignedMask = %s;" % mask)

def postloop():
    print("\t\t\tbreak;")


def func(cargo):
    f, last = cargo
    global is_binaryop, allow_ints
    if "func" in last:
        dummy, params = last.split("func:", 1)
        is_binaryop = False
        allow_ints = "ifunc" in last
    else:
        dummy, params = last.split("biop:", 1)
        is_binaryop = True
        allow_ints = "ibiop" in last

    params = params.strip().split()
    global def_unsigned_mask
    if len(params) > 0:
        nparams = int(params[0])
        if nparams > 26:
            raise ValueError("Number of parameters is greater than the supported 26!")
        def_unsigned_mask = any([ "u" in p for p in params[1:]])
    else:
        nparams = 0
        def_unsigned_mask = False

    while True:
        l = f.readline()
        if l == None:
            return eof, (f, l)
        l = l.strip(' ')
        name, jdoc = l.split(" - ", 1)
        jdoc = jdoc.strip()
        jextra = jdoc.split(';')
        if len(jextra) >= 1: # add new lines of javadocs
            edoc = jextra[1:]
            jdoc = jextra[0]
        else:
            edoc = None

        jbits = jdoc.split(',')
        if len(jbits) == 2: # escape expression
            doc = '{@code %s},%s' % tuple(jbits)
        else:
            doc = jdoc

        beginmethod(name, doc, edoc, nparams)
#        if len(plist) > 0: print("Parameters", plist)
        return cases, (f, '', name, jdoc, [])

def cases(cargo):
    f, last, name, jdoc, types = cargo
#    print("cases: |%s|, |%s|" % (name, last))
    while True:
        if last == None or len(last) > 0:
            l = last
            last = None
        else:
            l = f.readline()
        if l == None:
            endmethod(name, jdoc, types)
            return eof
        if len(l) > 0:
            code, out = whichcode(l)
            if code == None:
                if out == None:
                    endmethod(name, jdoc, types)
                    return func, (f, l)
                return parsefile, (f, l)
            return code, (f, out, name, jdoc, types)
        else:
            endmethod(name, jdoc, types)
            return eof, None


def whichcode(line):
    global is_binaryop
    typespec, out = line.split(":", 1)
    if typespec == "integer":
        return icode, out
    elif typespec == "integer_with_reals":
        return ircode, out
    elif typespec == "real":
        return rcode, out
    elif typespec == "complex":
        return ccode, out
    elif "func" in typespec or "biop" in typespec:
        return None, None
    else:
        return None, out

def getcode(f):
    text = []
    while True:
        l = f.readline()
        if l == None:
            break
        l = l.strip(' ')
        if len(l) == 0:
            break
        if l[0] == '\n':
            continue
        if l.find(":") >= 0:
            code, line = whichcode(l)
            if code != None:
                break
            if line == None:
                break
        text.append(l)

    return text, l

def icode(cargo):
    f, all, name, jdoc, types = cargo
    text, last = getcode(f)
#    print("int case:", name)
#    print(text)
    sameloop({ 8 : ("byte", "ByteDataset"), 16 : ("short", "ShortDataset"),
                32 : ("int", "IntegerDataset"), 64 : ("long", "LongDataset"), },
                "INT", "i", text, use_long=True, unsigned=def_unsigned_mask)
    types.append("integer")
    compoundloop({ 8 : ("byte", "CompoundByteDataset"), 16 : ("short", "CompoundShortDataset"),
                32 : ("int", "CompoundIntegerDataset"), 64 : ("long", "CompoundLongDataset"), },
                "ARRAYINT", "ai", text, use_long=True, unsigned=def_unsigned_mask)
    types.append("compound integer")
    return cases, (f, last, name, jdoc, types)

def ircode(cargo):
    f, all, name, jdoc, types = cargo
    text, last = getcode(f)
#    print("int case:", name)
#    print(text)
    sameloop({ 8 : ("byte", "ByteDataset"), 16 : ("short", "ShortDataset"),
                32 : ("int", "IntegerDataset"), 64 : ("long", "LongDataset"), },
                "INT", "i", text, override_long=True, unsigned=def_unsigned_mask)
    types.append("integer")
    compoundloop({ 8 : ("byte", "CompoundByteDataset"), 16 : ("short", "CompoundShortDataset"),
                32 : ("int", "CompoundIntegerDataset"), 64 : ("long", "CompoundLongDataset"), },
                "ARRAYINT", "ai", text, override_long=True, unsigned=def_unsigned_mask)
    types.append("compound integer")
    return cases, (f, last, name, jdoc, types)

def rcode(cargo):
    f, all, name, jdoc, types = cargo
    text, last = getcode(f)
#    print("real case:", name)
#    print(text)
    if "integer" not in types:
        sameloop({ 8 : ("byte", "ByteDataset"), 16 : ("short", "ShortDataset"),
                    32 : ("int", "IntegerDataset"), 64 : ("long", "LongDataset"), },
                    "INT", "i", text)
        types.append("integer")
        compoundloop({ 8 : ("byte", "CompoundByteDataset"), 16 : ("short", "CompoundShortDataset"),
                    32 : ("int", "CompoundIntegerDataset"), 64 : ("long", "CompoundLongDataset"), },
                    "ARRAYINT", "ai", text)
        types.append("compound integer")

    sameloop({ 32: ("float", "FloatDataset"), 64 : ("double", "DoubleDataset") },
                "FLOAT", "f", text)
    types.append("real")

    compoundloop({ 32: ("float", "CompoundFloatDataset"), 64 : ("double", "CompoundDoubleDataset") },
                "ARRAYFLOAT", "af", text)
    types.append("compound real")
    return cases, (f, last, name, jdoc, types)

def parse_b_real(text):
    new_text = []
    text_b_real = []
    has_b_real = False
    for t in text:
        if t.startswith('complex_b_real;'):
            has_b_real = True
        elif has_b_real:
            text_b_real.append(t)
        else:
            new_text.append(t)
            
    return new_text, text_b_real

def ccode(cargo):
    f, all, name, jdoc, types = cargo
    text, last = getcode(f)
#    print("comp case:", name)
#    print(text)
    if all.find("real") >= 0:
        real = True
    else:
        real = False

    text, text_b_real = parse_b_real(text)
    complexloop({ 64: ("float", "ComplexFloatDataset", "32"), 128 : ("double", "ComplexDoubleDataset", "64") },
                "COMPLEX", "c", text, text_b_real, real)
    types.append("complex")
    return cases, (f, last, name, jdoc, types)

def parsefile(cargo):
    f,last = cargo
    while True:
        if last == None or len(last) > 0:
            l = last
            last = None
        else:
            l = f.readline()
        if l == None:
            return eof, (f, l)
        l = l.strip(' ')
        if len(l) > 0:
            if l.find("func:") >= 0 or l.find("biop:") >= 0:
                return func, (f, l)
            raise ValueError("Line is not a function definition: %s" % l)

def eof(cargo):
    pass

def generatefunctions(funcs_file):
    m = StateMachine()
    m.add_state(parsefile)
    m.add_state(func)
    m.add_state(cases)
    m.add_state(icode)
    m.add_state(ircode)
    m.add_state(rcode)
    m.add_state(ccode)
    m.add_state(eof, end_state=1)
    m.set_start(parsefile)
    m.run((funcs_file, ''))

def generateclass(funcs, shell):
    while True:
        l = shell.readline()
        if l.startswith("package org.eclipse.january.dataset.internal.template;"):
            print("package org.eclipse.january.dataset;")
            break
        print(l, end='')
    while True: # edit imports
        l = shell.readline()
        if l.startswith("// start of imports that will be omitted in derived class"):
            break
        print(l, end='')
    print("import org.apache.commons.math3.complex.Complex;")
    while True: # edit imports
        l = shell.readline()
        if l.startswith("// end of imports that will be omitted in derived class"):
            break
    while True:
        l = shell.readline()
        if l.startswith("class MathsPreface {"):
            print("public class Maths {")
            break
        print(l, end='')
    while True:
        l = shell.readline()
        if l.startswith("// Start of generated code"):
            print(l, end='')
            break
        print(l, end='')

    generatefunctions(funcs)

    while True:
        l = shell.readline()
        if l.startswith("// End of generated code"):
            print(l, end='')
            break

    while True:
        l = shell.readline()
        if not l:
            break
        print(l, end='')

if __name__ == '__main__':
    import sys
    if len(sys.argv) > 1:
        fname = sys.argv[1]
    else:
        fname = "functions.txt"
    funcs_file = open(fname, 'r')

    if len(sys.argv) > 2:
        fname = sys.argv[2]
    else:
        fname = "MathsPreface.java"
    shell_file = open(fname, 'r')

    generateclass(funcs_file, shell_file)
