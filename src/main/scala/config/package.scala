package object config {

  /** Preserves `import` directive for a particular implicit.
   *
   * Sometimes IntelliJ cannot recognize that imported implicits are used in the scope and tries to wipe their imports
   * out on the imports optimization. Call this no-op method somewhere in the scope to tell IntelliJ about the required
   * implicits.
   */
  @inline def preserveImportsFor[A](implicit a: A): Unit = ()
}
