/**
 * Copyright (c) 2013 Saddle Development Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 **/

package org.saddle.mat

import scala.{specialized => spec}

import org.saddle._
import org.saddle.scalar._

/**
 * A Mat instance containing elements of type Double
 */
private[saddle] class MatDouble(r: Int, c: Int, values: Array[Double]) extends Mat[Double] {
  def repr = this

  def numRows = r

  def numCols = c

  def scalarTag = getScalarTag[Double]

  def map[@spec(Boolean, Int, Long, Double) B: CLM](f: (Double) => B): Mat[B] = MatImpl.map(this)(f)

  def foldLeft[@spec(Boolean, Int, Long, Double) A](init: A)(f: (A, Double) => A): A = MatImpl.foldLeft(this)(init)(f)

  // Cache the transpose: it's much faster to transpose and slice a continuous
  // bound than to take large strides, especially on large matrices where it
  // seems to eject cache lines on each stride (something like 10x slowdown)
  lazy val cachedT = {
    val matT = new MatDouble(numCols, numRows, values.clone())

    if (this.isSquare)
      MatMath.squareTranspose(matT)
    else
      MatMath.blockTranspose(this, matT)

    matT
  }

  def transposed = cachedT

  def copy: Mat[Double] = new MatDouble(numRows, numCols, values.clone())

  def takeRows(locs: Int*): Mat[Double] = MatImpl.takeRows(this, locs.toSet)

  def withoutRows(locs: Int*): Mat[Double] = MatImpl.withoutRows(this, locs.toSet)

  def reshape(r: Int, c: Int): Mat[Double] = new MatDouble(r, c, values)

  // access like vector in row-major order
  private[saddle] def apply(i: Int) = values(i)

  // implement access like matrix(i, j)
  private[saddle] def apply(r: Int, c: Int) = apply(r * numCols + c)

  // updates values: use with caution!
  private[saddle] def update(i: Int, v: Double) {
    values(i) = v
  }

  // use with caution, may not return copy
  private[saddle] def toArray: Array[Double] = values

  // use with caution, may not return copy
  private[saddle] def toDoubleArray(implicit ev: NUM[Double]): Array[Double] = values

  /** Row-by-row equality check of all values. */
  override def equals(o: Any): Boolean = o match {
    case rv: Mat[_] => (this eq rv) || this.numRows == rv.numRows && this.numCols == rv.numCols && {
      var i = 0
      var eq = true
      while(eq && i < length) {
        eq &&= (apply(i) == rv(i) || this.scalarTag.isMissing(apply(i)) && rv.scalarTag.isMissing(rv(i)))
        i += 1
      }
      eq
    }
    case _ => super.equals(o)
  }
}
