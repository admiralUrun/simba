package services.importer
import models.Recipe
import com.github.tototoshi.csv.CSVReader
import java.io.{File, FileOutputStream, PrintWriter}

import scala.annotation.tailrec
class OrderImporter extends Importer {
  private val recipeProperties = Map(
    "id" -> "id",
    "name" -> "name"
  )
  private val ingredientsProperties = Map(
    "id" -> "id",
    "description" -> "description",
    "unit" -> "unit",
    "artBy" -> "art_by"
  )
  private val recipeIngredientsProperties = Map(
    "recipeId" -> "recipe_id",
    "ingredientId" -> "ingredient_id",
    "netto" -> "netto",
    "brutto" -> "brutto"
  )
  def importAllFromCSV(csv: File, recipes: File, ingredients: File, recipeIngredients: File): Unit = {
    val lines = CSVReader.open(csv).all().toArray
    def writer(printWriter: PrintWriter, lines: List[SQLCommand]): Unit = {
      lines.foreach(l => printWriter.write(l))
      printWriter.close()
    }
    def importRecipe: List[SQLCommand] = List(
        startOfSQLCommand("recipes") + generateInsertingProperties(List("name"), recipeProperties) + generateInsertingVariables(List(lines(1).head)) + endOfSQLCommand,
      "\n"
      )
    def importIngredients: List[SQLCommand] = {
      def getAllSQLCommands(generator: Ingredient => SQLCommand, i: Int, until: Int): List[SQLCommand] = {
        @tailrec
        def getAllSQLCommands(generator: Ingredient => SQLCommand, i: Int, until: Int, acc: List[SQLCommand]): List[SQLCommand] = {
          if(i >= until) acc
          else getAllSQLCommands(generator, i + 1, until, generator(getIngredientFromLine(lines(i).toArray)) +: acc)
        }
        getAllSQLCommands(generator, i, until, List("\n"))
      }
      def getIngredientFromLine(a: Array[String]): Ingredient = {
        Ingredient(null, a(2), a(3), a(1).toInt)
      }
      def generateInsertForIngredients(ingredient: Ingredient): SQLCommand = {
        startOfSQLCommand("ingredients") +
          generateInsertingProperties(getProperties(List("id", "description", "unit", "artBy"), ingredient.productIterator), ingredientsProperties) +
          generateInsertingVariables(getVariables(ingredient.productIterator)) +
          endOfSQLCommand
      }
      getAllSQLCommands(generateInsertForIngredients, 4, lines.length - 1)
    }
    def importRecipeIngredients: List[SQLCommand] = {
      def getRecipeIngredientsFromLine(a: Array[String]): (RecipeIngredients, String) = {
        (RecipeIngredients(Option(1), Option(2), a(4), a(5)), a(2))
      }
      def generateRecipeIngredientsForIngredients(recipeIngredients: (RecipeIngredients, String), recipe: String): SQLCommand = {
        startOfSQLCommand("recipe_ingredients") +
          generateInsertingProperties(getProperties(List("recipeId", "ingredientId", "netto", "brutto"), recipeIngredients._1.productIterator), recipeIngredientsProperties) +
          generateInsertingVariables(getVariables(recipeIngredients._1.productIterator)) +
        s"; -- $recipe ingredient ${recipeIngredients._2} \n"
      }
      def getAllSQLCommands(generator: ((RecipeIngredients, String), String) => SQLCommand, i: Int, until: Int): List[SQLCommand] = {
       @tailrec
       def getAllSQLCommands(generator: ((RecipeIngredients, String), String) => SQLCommand, i: Int, until: Int, acc: List[SQLCommand]): List[SQLCommand] = {
         if(i >= until)
           acc
         else getAllSQLCommands(generator, i + 1, until, generator(getRecipeIngredientsFromLine(lines(i).toArray), lines(1).head) +: acc)
       }
        getAllSQLCommands(generator, i, until, List("\n"))
      }
      getAllSQLCommands(generateRecipeIngredientsForIngredients, 4, lines.length - 1)
    }

//    writer(new PrintWriter(new FileOutputStream(recipes, true)), importRecipe)
//    writer(new PrintWriter(new FileOutputStream(ingredients, true)), importIngredients)
    writer(new PrintWriter(new FileOutputStream(recipeIngredients, true)), importRecipeIngredients)
  }
}
object OrderImporter extends OrderImporter with App {
  val recipes = new File("/Users/andrewyakovenko/Programming/On gitHub/simba/sql/recipes.sql")
  val ingredients = new File("/Users/andrewyakovenko/Programming/On gitHub/simba/sql/ingredients.sql")
  val recipeIngredients = new File("/Users/andrewyakovenko/Programming/On gitHub/simba/sql/recipe-ingredients.sql")
  def importFrom(file: File): Unit = {
    importAllFromCSV(
      file,
      recipes,
      ingredients,
      recipeIngredients
    )
  }

  getAllFilesInDirectory(new File("/Users/andrewyakovenko/Downloads/Import Resipes/brackfast 44")).foreach(importFrom)
  getAllFilesInDirectory(new File("/Users/andrewyakovenko/Downloads/Import Resipes/calassicc 44")).foreach(importFrom)
  getAllFilesInDirectory(new File("/Users/andrewyakovenko/Downloads/Import Resipes/lite 44")).foreach(importFrom)
  importFrom(new File("/Users/andrewyakovenko/Downloads/Import Resipes/Калькуляция Профитроли с творожным кремом.xlsx - TDSheet.csv"))
  importFrom(new File("/Users/andrewyakovenko/Downloads/Import Resipes/Калькуляция Тыквенный суп.xlsx - TDSheet.csv"))
}
// case class Recipe(id: Int, name: String)
case class Ingredient(id: Option[Int], description: String, unit: String, artBy: Int)
case class RecipeIngredients(recipeId: Option[Int], ingredientId: Option[Int], netto: String, brutto: String)
