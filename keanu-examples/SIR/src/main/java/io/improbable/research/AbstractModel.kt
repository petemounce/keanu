package io.improbable.research

import io.improbable.keanu.tensor.dbl.DoubleTensor
import io.improbable.keanu.vertices.dbl.nonprobabilistic.diff.DualNumber
import org.apache.commons.math3.distribution.PoissonDistribution
import org.apache.commons.math3.random.MersenneTwister

class AbstractModel(var rhoS: Double, var rhoI: Double, var rhoR: Double) {
    val Nsamples = 10000 // number of samples of the concrete model
    val rand = MersenneTwister()
    var concreteStates = arrayOf<SIRModel>()

//    constructor(T: DoubleTensor) :
//        this(T.getValue(0), T.getValue(1), T.getValue(2)) {
////        println("AbstractModel created with rhoS = $rhoS, rhoI = $rhoI, rhoR = $rhoR")
//
//    }

    fun step() {
        concreteStates.forEach { model -> model.step() }
        setStateFromConcreteSamples(concreteStates)
    }

//    fun step(startState: DoubleTensor): DoubleTensor {
//        setStateFromTensor(startState)
//        step()
//        return DoubleTensor.create(doubleArrayOf(rhoS, rhoI, rhoR))
//    }

//    fun step(startS: Double, startI: Double, startR: Double): DoubleArray {
//        setState(startS, startI, startR)
//        step()
//        return doubleArrayOf(rhoS, rhoI, rhoR)
//    }

//    fun calculateDualNumber(inDual: DualNumber?): DualNumber? {
//        if (inDual == null) return null
//
////        println("calculating jacobian at ${inDual.value}")
//        setStateFromTensor(inDual.value)
//
//        setConcreteStates()
//        val inConcreteStates = asMatrix(concreteStates)  // 3xNsamples matrix
//        concreteStates.forEach { it.step() }
//        setStateFromConcreteSamples(concreteStates)
//        val outConcreteStates = asMatrix(concreteStates)
////        println("State at end of step is ${getStateAsTensor()}")
//
//        val jacobian = calculateJacobian(inConcreteStates, outConcreteStates, inDual.value)
//
//        val values = DoubleTensor.create(doubleArrayOf(rhoS, rhoI, rhoR))
//
//        val partialDerivatives = inDual.partialDerivatives.asMap().mapValues {
//            jacobian.tensorMultiply(it.value, intArrayOf(1), intArrayOf(1)).reshape(1, 3, 1, 3)
//        }
//
//        val dual = DualNumber(values, partialDerivatives)
//
//        return dual
//    }

//    fun setConcreteStates() {
//        if (concreteStates.isEmpty()) {
//            createConcreteStates()
//        }
//
//        if (upstreamAbstractModel == null) {
//            println("Sampling concrete states from abstract state (no upstream abstract model)")
//            val sPoisson = PoissonDistribution(rhoS)
//            val iPoisson = PoissonDistribution(rhoI)
//            val rPoisson = PoissonDistribution(rhoR)
//
//            for (i in 0 until Nsamples) {
//                val concreteState = concreteStates[i]
//                concreteState.S = sPoisson.sample()
//                concreteState.I = iPoisson.sample()
//                concreteState.R = rPoisson.sample()
//            }
//        } else {
//            println("Setting concrete states from upstream")
//            for (i in 0 until Nsamples) {
//                val upstreamConcreteState = upstreamAbstractModel!!.concreteStates[i]
//                val concreteState = concreteStates[i]
//                concreteState.S = upstreamConcreteState.S
//                concreteState.I = upstreamConcreteState.I
//                concreteState.R = upstreamConcreteState.R
//            }
//        }
//    }

    fun createConcreteStates() {
        println("Creating concrete states")
        val sPoisson = PoissonDistribution(rhoS)
        val iPoisson = PoissonDistribution(rhoI)
        val rPoisson = PoissonDistribution(rhoR)

        concreteStates = Array(Nsamples, {
            SIRModel(
                sPoisson.sample(),
                iPoisson.sample(),
                rPoisson.sample(),
//                PoissonDistribution(rhoS).sample(),
//                PoissonDistribution(rhoI).sample(),
//                PoissonDistribution(rhoR).sample(),
                rand
            )
        })
    }

//    fun calculateJacobianTensor(inConcreteStates: DoubleTensor, outConcreteStates: DoubleTensor, inDualValue: DoubleTensor): DoubleTensor {
//        val a = (inConcreteStates.sum(1) / inDualValue) / Nsamples.toDouble()
//        val jacobian =
//            ((outConcreteStates.reshape(3, 1, Nsamples)).tensorMultiply(inConcreteStates.reshape(1, 3, Nsamples), intArrayOf(1), intArrayOf(0)).sum(2) * inDualValue.reciprocal() -
//                outConcreteStates.sum(1).matrixMultiply(a)) / Nsamples.toDouble()
//
//        return jacobian
//    }

    fun calculateJacobian(inConcreteStates: DoubleTensor, outConcreteStates: DoubleTensor, inAbstractState: DoubleTensor): DoubleTensor {
        val jacobian = DoubleTensor.zeros(intArrayOf(3, 3))

        for (i in 0..2) {
            for (j in 0..2) {
                val a = inConcreteStates.sum(1).getValue(j) / Nsamples.toDouble()

                var element = 0.0
                for (k in 0 until Nsamples) {
                    element += outConcreteStates.getValue(i, k)*(inConcreteStates.getValue(j, k) - a) /
                        (Nsamples.toDouble() * inAbstractState.getValue(0, j))
                }
                jacobian.setValue(element, i, j)
            }
        }

        return jacobian
    }

//    fun setStateFromTensor(T: DoubleTensor) {
//        rhoS = T.getValue(0)
//        rhoI = T.getValue(1)
//        rhoR = T.getValue(2)
//    }

    fun setState(s: Double, i: Double, r: Double) {
        rhoS = s
        rhoI = i
        rhoR = r
    }

//    fun getStateAsTensor(): DoubleTensor {
//        return DoubleTensor.create(doubleArrayOf(rhoS, rhoI, rhoR))
//    }

    fun getStateAsArray(): Array<DoubleTensor> {
        return arrayOf(DoubleTensor.scalar(rhoS), DoubleTensor.scalar(rhoI), DoubleTensor.scalar(rhoR))
    }

    fun setStateFromConcreteSamples(concreteStates : Array<SIRModel>) {
        rhoS = concreteStates.sumBy { model -> model.S } / Nsamples.toDouble()
        rhoI = concreteStates.sumBy { model -> model.I } / Nsamples.toDouble()
        rhoR = concreteStates.sumBy { model -> model.R } / Nsamples.toDouble()
    }

//    fun asMatrix(samples: Array<SIRModel>): DoubleTensor {
//        val s = DoubleTensor.zeros(intArrayOf(3, samples.size))
//        for (i in 0 until samples.size) {
//            s.setValue(samples[i].S.toDouble(), 0, i)
//            s.setValue(samples[i].I.toDouble(), 1, i)
//            s.setValue(samples[i].R.toDouble(), 2, i)
//        }
//        return s
//    }
}