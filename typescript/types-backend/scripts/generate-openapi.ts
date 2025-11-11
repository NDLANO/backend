/*
 * Part of NDLA backend
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

import fs from "node:fs";
import openapiTS, { astToString, TransformObject } from "openapi-typescript";
import ts, { TypeNode } from "typescript";

if (process.argv.length !== 3) {
  throw new Error("Invalid use");
}

const BLOB = ts.factory.createTypeReferenceNode(
  ts.factory.createIdentifier("Blob"),
);
const NULL = ts.factory.createLiteralTypeNode(ts.factory.createNull()); // `null`

async function generate_types(appName: string) {
  const jsonFile = `./openapi/${appName}.json`;
  console.log(`Parsing ${jsonFile} to generate typescript files...`);
  const schema = await fs.promises.readFile(jsonFile, "utf8");
  const schemaContent = JSON.parse(schema);

  const ast = await openapiTS(schemaContent, {
    exportType: true,
    // https://openapi-ts.dev/migration-guide#defaultnonnullable-true-by-default
    defaultNonNullable: false,
    transform(schemaObject, _options): TypeNode | undefined {
      if (schemaObject.format === "binary") {
        if (schemaObject.nullable) {
          return ts.factory.createUnionTypeNode([BLOB, NULL]);
        } else {
          return BLOB;
        }
      }
    },
  });

  const outputPath = `./${appName}-openapi.ts`;
  const output = astToString(ast);

  console.log(`Outputting to ${outputPath}`);
  fs.writeFileSync(outputPath, output);

  let fileContent = `// This file is generated automatically. Do not edit.
import * as openapi from "./${appName}-openapi";
type schemas = openapi.components["schemas"];
export { openapi };

`;

  const schemas = schemaContent.components.schemas;
  const schemaNames = Object.keys(schemas);
  for (const schemaName of schemaNames) {
    fileContent += `export type ${schemaName} = schemas["${schemaName}"];\n`;
  }

  const apiTypesFile = `./${appName}.ts`;
  console.log(`Outputting to ${apiTypesFile}`);
  fs.writeFileSync(apiTypesFile, fileContent);
}

generate_types(process.argv[2]);
