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

  const ast = await openapiTS(JSON.parse(schema), {
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
}

generate_types(process.argv[2]);
