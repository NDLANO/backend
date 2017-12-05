# LANGUAGE
Library for handling language in APIs from Global Digital Library.

# Usage
Add dependency to this library: `"gdl" %% "language" % "<version>",`

The main way of using this library is with the case class `LanguageTag`.
It accepts a string as input on the format "LanguageCode-Script-Region"
* _LanguageCode_ is required, and supports iso639-1/2/3
* _Script_ is optional, but if used must be a valid iso1924 value
* _Region_ is optional, but if used must be a valid iso3155 value

A `LanguageNotSupportedException` will be thrown if the string given as input is not valid.

Example:

    class MyClass {
       def doSomeLanguageHandling = {
          val tag1 = LanguageTag("amh")
          val tag2 = LanguageTag("en-latn-gb")
          val tag3 = LanguageTag("eng-gb")
          
          println(tag1)
          println(tag1.displayName)
          println(tag2)
          println(tag2.displayName)
          println(tag3) 
          println(tag3.displayName)
       }
    }
    
Output from the above println

    "amh"
    "Amharic"
    "eng-latn-gb"
    "English (Latin, United Kingdom)
    "eng-gb"
    "English (United Kingdom)"
 

# Building and distribution

## Updating the library, when standards change
The following files are the used as input to this library:

* iso-639-3_20170202.tab - Can be downloaded at [http://www-01.sil.org/iso639-3/download.asp]
* iso-3166-2.csv - Can be downloaded at [https://datahub.io/core/country-list]
* iso15924-utf8-20170726.txt - Can be downloaded at [http://unicode.org/iso15924/codelists.html]

To update this library, download the above mentioned files, and run ./generator/generate.py

This will regenerate the content of CodeLists and adjacent files.

## Compile
    sbt compile

## Run tests
    sbt test

## Build
    ./build.sh

## Publish
    ./release.sh
