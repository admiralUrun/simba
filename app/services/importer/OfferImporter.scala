package services.importer

import com.github.tototoshi.csv.CSVReader
import java.io.{File, FileOutputStream, PrintWriter}
import scala.annotation.tailrec
import cats.effect.IO
import cats.implicits._

class OfferImporter extends Importer {
  private val recipeProperties = Map(
    "id" -> "id",
    "name" -> "name",
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

  def getImportEffectForAllGivenCSVs(CSVs: List[File], recipes: File, ingredients: File, recipeIngredients: File): IO[Unit] = {
    @tailrec
    def getImportEffectForAllGivenCSVs(CSVs: List[File], recipes: File, ingredients: File, recipeIngredients: File, newId: Int, acc: IO[Unit]): IO[Unit] = {
      def importAllFromCSV(csv: File, recipes: File, ingredients: File, recipeIngredients: File, recipeId: Int): IO[Unit] = {
        val lines = CSVReader.open(csv).all().toArray

        def writerLinesIntoGivenPintWriter(printWriter: PrintWriter, lines: List[SQLCommand]): Unit = {
            lines.foreach(l => printWriter.write(l))
            printWriter.close()
        }

        def importRecipe(recipesId: Int): List[SQLCommand] = {
          def getRecipe(id: Int, name: String): RecipeForImporter = RecipeForImporter(id, name, 0)
          List(
            startOfSQLCommand("recipes")
              + generateInsertingProperties(List("id", "name", "edited"), recipeProperties)
              + generateInsertingVariables(getRecipe(recipesId, lines(1).head).productIterator.map(_.toString).toList)
              + endOfSQLCommand,
            "\n"
          )
        }

        def importIngredients: List[SQLCommand] = {
          def getAllSQLCommands(generator: IngredientForImporter => SQLCommand, i: Int, until: Int): List[SQLCommand] = {
            @tailrec
            def getAllSQLCommands(generator: IngredientForImporter => SQLCommand, i: Int, until: Int, acc: List[SQLCommand]): List[SQLCommand] = {
              if (i >= until) acc
              else getAllSQLCommands(generator, i + 1, until, generator(getIngredientFromLine(lines(i).toArray)) +: acc)
            }

            getAllSQLCommands(generator, i, until, List("\n"))
          }

          def getIngredientFromLine(a: Array[String]): IngredientForImporter = {
            IngredientForImporter(a(1).toInt, a(2), a(3), a(1).toInt, 0)
          }

          def generateInsertForIngredients(ingredient: IngredientForImporter): SQLCommand = {
            startOfSQLCommand("ingredients") +
              generateInsertingProperties(getProperties(List("id", "description", "unit", "artBy", "edited"), ingredient.productIterator), ingredientsProperties) +
              generateInsertingVariables(getVariables(ingredient.productIterator)) +
              endOfSQLCommand
          }

          getAllSQLCommands(generateInsertForIngredients, 4, lines.length - 1)
        }

        def importRecipeIngredients(recipesId: Int): List[SQLCommand] = {
          def getRecipeIngredientsFromLine(a: Array[String]): RecipeIngredientsForImporter = {
            RecipeIngredientsForImporter(recipesId, a(1).toInt, a(4).replace(',', '.'))
          }

          def generateRecipeIngredientsForIngredients(recipeIngredients: RecipeIngredientsForImporter, recipe: String): SQLCommand = {
            startOfSQLCommand("recipe_ingredients") +
              generateInsertingProperties(
                getProperties(List("recipeId", "ingredientId", "netto", "brutto"), recipeIngredients.productIterator), recipeIngredientsProperties) +
              generateInsertingVariables(getVariables(recipeIngredients.productIterator)) +
              endOfSQLCommand
          }

          def getAllSQLCommands(generator: (RecipeIngredientsForImporter, String) => SQLCommand, i: Int, until: Int): List[SQLCommand] = {
            @tailrec
            def getAllSQLCommands(generator: (RecipeIngredientsForImporter, String) => SQLCommand, i: Int, until: Int, acc: List[SQLCommand]): List[SQLCommand] = {
              if (i >= until)
                acc
              else getAllSQLCommands(generator, i + 1, until, generator(getRecipeIngredientsFromLine(lines(i).toArray), lines(1).head) +: acc)
            }

            getAllSQLCommands(generator, i, until, List("\n"))
          }

          getAllSQLCommands(generateRecipeIngredientsForIngredients, 4, lines.length - 1)
        }

        IO{
          writerLinesIntoGivenPintWriter(new PrintWriter(new FileOutputStream(recipes, true)), importRecipe(recipeId))
          writerLinesIntoGivenPintWriter(new PrintWriter(new FileOutputStream(ingredients, true)), importIngredients)
          writerLinesIntoGivenPintWriter(new PrintWriter(new FileOutputStream(recipeIngredients, true)), importRecipeIngredients(recipeId))
        }
      }
      CSVs match {
        case h :: t => getImportEffectForAllGivenCSVs(t, recipes, ingredients, recipeIngredients, newId + 1, acc *> importAllFromCSV(h, recipes, ingredients, recipeIngredients, newId + 1))
        case List() => acc
      }
    }
    getImportEffectForAllGivenCSVs(CSVs, recipes, ingredients, recipeIngredients, 1, IO())
  }

}



object OfferImporter extends OfferImporter with App {
  val recipes = new File("/Users/andrewyakovenko/Programming/On gitHub/simba/sql/recipes.sql")
  val ingredients = new File("/Users/andrewyakovenko/Programming/On gitHub/simba/sql/ingredients.sql")
  val recipeIngredients = new File("/Users/andrewyakovenko/Programming/On gitHub/simba/sql/recipe-ingredients.sql")
  val importEffect = getImportEffectForAllGivenCSVs(getAllFilesInDirectory(???), recipes, ingredients, recipeIngredients)
  importEffect.unsafeRunSync()
}

case class RecipeForImporter(id: Int, name: String, edited: Int)
case class IngredientForImporter(id: Int, description: String, unit: String, artBy: Int, edited: Int)
case class RecipeIngredientsForImporter(recipeId: Int, ingredientId: Int, netto: String)
