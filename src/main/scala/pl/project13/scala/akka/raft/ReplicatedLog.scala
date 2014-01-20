package pl.project13.scala.akka.raft

import akka.actor.ActorRef
import scala.collection.immutable

case class ReplicatedLog[T <: AnyRef](
  entries: List[Entry[T]],
  commitedIndex: Int,
  lastApplied: Int
) {

  /**
   * Performs the "consistency check", which basically checks if we have an entry for this pair of values
   *
   * This is O(n) and if the reason why we'll want to compact/snapshot the log.
   */
  def isConsistentWith(index: Int, term: Term) =
    entries.view.zipWithIndex exists { case (e, i) => e.term == term && i == index }

  // log state
  def lastIndex: Int = entries.length - 1
  def lastTerm: Term = entries.maxBy(_.term.termNr).term

  // log actions
  def commit(n: Int) = copy(commitedIndex = n) // todo persist too, right?
  def append(term: Term, command: T, client: Option[ActorRef]): ReplicatedLog[T] =
    copy(entries = Entry(command, term, client) :: entries)

  // log views
  // todo inverse, because we prepend, not append
  def entriesFrom(idx: Int, howMany: Int = 5) = entries.slice(idx, idx + howMany)
  def notCommittedEntries = entries.slice(commitedIndex + 1, entries.length)

}

object ReplicatedLog {
  def empty[T <: AnyRef] = ReplicatedLog[T](List(Entry(null.asInstanceOf[T], Term(0), None)), 0, 0)
}

case class Entry[T <: AnyRef](
  command: T,
  term: Term,
  client: Option[ActorRef] = None
)