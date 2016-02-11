import scala.concurrent.{Future, Await}
import scala.concurrent.duration.Duration
import slick.driver.H2Driver.api._
import scala.concurrent.ExecutionContext.Implicits.global

object LiftedEmbedding extends App {

  case class Supplier(id: Int, name: String, string: String, city: String, state: String, zip: String)

  class Suppliers(tag: Tag)
    extends Table[Supplier](tag, "SUPPLIERS") {

    // This is the primary key column:
    def id = column[Int]("SUP_ID", O.PrimaryKey)
    def name = column[String]("SUP_NAME")
    def street = column[String]("STREET")
    def city = column[String]("CITY")
    def state = column[String]("STATE")
    def zip = column[String]("ZIP")

    // Every table needs a * projection with the same type as the table's type parameter
    def * = (id, name, street, city, state, zip) <> (Supplier.tupled, Supplier.unapply)
  }

  val suppliers = TableQuery[Suppliers]



  case class Coffee(name: String, supplierId: Int, price: Double, sales: Int, total: Int)

  class Coffees(tag: Tag) extends Table[Coffee](tag, "COFFEES") {
    def name = column[String]("COF_NAME")
    def price = column[Double]("PRICE")
    def supID = column[Int]("SUP_ID")
    def sales = column[Int]("SALES", O.Default(0))
    def total = column[Int]("TOTAL", O.Default(0))

    def * = (name, supID, price, sales, total) <> (Coffee.tupled, Coffee.unapply)
  }

  val coffees = TableQuery[Coffees]

  def populate(): DBIO[Unit] =
    DBIO.seq(
      suppliers += Supplier(101, "Acme, Inc.", "99 Market Street", "Groundsville", "CA", "95199"),
      suppliers += Supplier( 49, "Superior Coffee", "1 Party Place", "Mendocino", "CA", "95460"),
      suppliers += Supplier(150, "The High Ground", "100 Coffee Lane", "Meadows", "CA", "93966"),
      coffees ++= Seq (
        Coffee("Colombian",         101, 7.99, 0, 0),
        Coffee("French_Roast",       49, 8.99, 0, 0),
        Coffee("Espresso",          150, 9.99, 0, 0),
        Coffee("Colombian_Decaf",   101, 8.99, 0, 0),
        Coffee("French_Roast_Decaf", 49, 9.99, 0, 0)
      )
    )


  def example = {
    coffees.result
  }


  val db: Database = Database.forConfig("h2")

  try {
    val schema = coffees.schema ++ suppliers.schema

    val result =
      Await.result( db.run(schema.create andThen populate andThen example), Duration.Inf)

    println(s"Result: $result")
  } finally db.close
}
