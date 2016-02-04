package com.utamars

package object util {

  implicit class IntOps(i: Int) {
    def between(a: Int, b: Int): Boolean = a to b contains i
  }
}
