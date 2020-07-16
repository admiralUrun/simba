package services.importer

import java.io.{File, FileOutputStream, PrintWriter}
import scala.annotation.tailrec
import cats.effect.IO
import cats.implicits._

class OfferImporter extends Importer {
  private val recipeProperties = Map(
    "id" -> "id",
    "name" -> "name",
    "menuType" -> "type",
    "edited" -> "edited"
  )
  private val ingredientsProperties = Map(
    "id" -> "id",
    "description" -> "description",
    "unit" -> "unit",
    "artBy" -> "art_by",
    "edited" -> "edited"
  )
  private val recipeIngredientsProperties = Map(
    "recipeId" -> "recipe_id",
    "ingredientId" -> "ingredient_id",
    "netto" -> "netto"
  )
  def getImportEffectForAllGivenCSVs(files: List[File], recipes: File, ingredients: File, recipeIngredients: File, startId: Int, menuType: String, reader: File => Lines): IO[Unit] = {
    @tailrec
    def getImportEffectForAllGivenCSVs(files: List[File], recipes: File, ingredients: File, recipeIngredients: File, newId: Int, reader: File => Lines, acc: IO[Unit]): IO[Unit] = {
      def importAllFromCSV(file: File, recipes: File, ingredients: File, recipeIngredients: File, recipeId: Int, reader: File => Lines): IO[Unit] = {
        val lines = reader(file)

        def getAllSQLCommands[T](lines: Lines, generator: T => SQLCommand, getTFromLine: Array[String] => T , i: Int, until: Int): List[SQLCommand] = {
          @tailrec
          def getAllSQLCommands(generator: T => SQLCommand, i: Int, until: Int, acc: List[SQLCommand]): List[SQLCommand] = {
            if (i >= until) acc
            else getAllSQLCommands(generator, i + 1, until, generator(getTFromLine(lines(i).toArray)) +: acc)
          }

          getAllSQLCommands(generator, i, until, List("\n"))
        }

        def writerLinesIntoGivenPintWriter(printWriter: PrintWriter, lines: List[SQLCommand]): Unit = {
            lines.foreach(l => printWriter.write(l))
            printWriter.close()
        }

        def importRecipe(recipesId: Int): List[SQLCommand] = {
          def getRecipe(id: Int, name: String): RecipeForImporter = RecipeForImporter(id, name, menuType, 0)
          List(
            startOfSQLCommand("recipes")
              + generateInsertingProperties(List("id", "name", "menuType", "edited"), recipeProperties)
              + generateInsertingVariables(getRecipe(recipesId, lines(1).head).productIterator.map(_.toString).toList)
              + endOfSQLCommand,
            "\n"
          )
        }

        def importIngredients: List[SQLCommand] = {

          def getIngredientFromLine(a: Array[String]): IngredientForImporter = {
            IngredientForImporter(a(1).toInt, a(2), a(3), a(1).toInt, 0)
          }

          def generateInsertForIngredients(ingredient: IngredientForImporter): SQLCommand = {
            startOfSQLCommand("ingredients") +
              generateInsertingProperties(getProperties(List("id", "description", "unit", "artBy", "edited"), ingredient.productIterator), ingredientsProperties) +
              generateInsertingVariables(getVariables(ingredient.productIterator)) +
              endOfSQLCommand
          }

          getAllSQLCommands(lines, generateInsertForIngredients, getIngredientFromLine, 4, lines.length - 1)
        }

        def importRecipeIngredients(recipesId: Int): List[SQLCommand] = {
          def getRecipeIngredientsFromLine(a: Array[String]): RecipeIngredientsForImporter = {
            RecipeIngredientsForImporter(recipesId, a(1).toInt, a(4).replace(',', '.'))
          }

          def generateRecipeIngredientsForIngredients(recipeIngredients: RecipeIngredientsForImporter): SQLCommand = {
            startOfSQLCommand("recipe_ingredients") +
              generateInsertingProperties(
                getProperties(List("recipeId", "ingredientId", "netto", "brutto"), recipeIngredients.productIterator), recipeIngredientsProperties) +
              generateInsertingVariables(getVariables(recipeIngredients.productIterator)) +
              endOfSQLCommand
          }

          getAllSQLCommands(lines, generateRecipeIngredientsForIngredients, getRecipeIngredientsFromLine, 4, lines.length - 1)
        }

        IO{
          writerLinesIntoGivenPintWriter(new PrintWriter(new FileOutputStream(recipes, true)), importRecipe(recipeId))
          writerLinesIntoGivenPintWriter(new PrintWriter(new FileOutputStream(ingredients, true)), importIngredients)
          writerLinesIntoGivenPintWriter(new PrintWriter(new FileOutputStream(recipeIngredients, true)), importRecipeIngredients(recipeId))
        }
      }
      files match {
        case h :: t => getImportEffectForAllGivenCSVs(t, recipes, ingredients, recipeIngredients, newId + 1, reader, acc *> importAllFromCSV(h, recipes, ingredients, recipeIngredients, newId, reader))
        case List() => acc
      }
    }
    getImportEffectForAllGivenCSVs(files, recipes, ingredients, recipeIngredients, startId, reader, IO(println("Done")))
  }

}



object OfferImporter extends OfferImporter with App {
  val recipes = new File("sql/recipes.sql")
  val ingredients = new File("sql/ingredients.sql")
  val recipeIngredients = new File("sql/recipe-ingredients.sql")

  val classics = getAllXLSXsInDirectory(new File("/Users/andrewyakovenko/Downloads/Классик"))
  val breakfasts = getAllXLSXsInDirectory(new File("/Users/andrewyakovenko/Downloads/сніданок"))
  val lites = getAllXLSXsInDirectory(new File("/Users/andrewyakovenko/Downloads/лайт"))
  val deserts = getAllXLSXsInDirectory(new File("/Users/andrewyakovenko/Downloads/десерт"))
  val soups = getAllXLSXsInDirectory(new File("/Users/andrewyakovenko/Downloads/суп"))

  val importEffect: IO[Unit] = {
    impotEffectFrom(classics, "classic", 1) *>
      impotEffectFrom(breakfasts, "breakfast", classics.length + 1) *>
      impotEffectFrom(lites, "lite", classics.length + breakfasts.length + 1) *>
      impotEffectFrom(deserts, "desert", classics.length + breakfasts.length + lites.length + 1) *>
      impotEffectFrom(soups, "soup", classics.length + breakfasts.length + lites.length + deserts.length + 1)
  }
  def impotEffectFrom(files: List[File], menuType: String, startId: Int): IO[Unit] = {
    getImportEffectForAllGivenCSVs(files, recipes, ingredients, recipeIngredients, startId, menuType, xlsxToLines)
  }


  importEffect.unsafeRunSync()
}

case class RecipeForImporter(id: Int, name: String, menuType: String,  edited: Int)
case class IngredientForImporter(id: Int, description: String, unit: String, artBy: Int, edited: Int)
case class RecipeIngredientsForImporter(recipeId: Int, ingredientId: Int, netto: String)
