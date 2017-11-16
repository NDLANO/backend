# LANGUAGE
Library for handling language in APIs from Global Digital Library.

# Usage
Add dependency to this library: `"gdl" %% "language" % "<version>",`

The main method of using this library is with the trait `LanguageSupport` and with the `LanguageTag` case class.

Example:

    class MyService extends LanguageSupport {
       override implicit val languageProvider: LanguageProvider = new LanguageProvider(new Iso639, new Iso3166, new Iso15924)
       
       def asLanguageTag(asString: String): LanguageTag = {
          LanguageTag.fromString(asString)
       }
    }
    
Example in an environment with Cake-pattern:

    object ComponentRegistry extends MyController with LanguageSupport {
        lazy val myController = new MyController
        implicit lazy val languageProvider: LanguageProvider = new LanguageProvider(new Iso639, new Iso3166, new Iso15924)
    }
     
    
    trait MyController {
       this: LanguageSupport =>
       val myController: MyController
       
       class MyController extends ScalatraServlet with NativeJsonSupport {
          get("/:lang/") {
            val langAsString = params("lang")
            val validatedLanguage = LanguageTag.fromString(langAsString)
            
            // do something with language
          }
       }
    }
 

# Building and distribution

## Compile
    sbt compile

## Run tests
    sbt test

## Build
    ./build.sh

## Publish
    ./release.sh
