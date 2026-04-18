/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

import fs from "node:fs";
import openapiTS, { astToString } from "openapi-typescript";

if (process.argv.length !== 3) {
  throw new Error("Invalid use");
}

async function generate_types(appName: string) {
  const jsonFile = `./openapi/${appName}.json`;
  console.log(`Parsing ${jsonFile} to generate typescript files...`);
  const schema = await fs.promises.readFile(jsonFile, "utf8");
  const schemaContent = JSON.parse(schema);

  const ast = await openapiTS(schemaContent, {
    defaultNonNullable: false,
    exportType: true,
    rootTypes: true,
    rootTypesKeepCasing: true,
    rootTypesNoSchemaPrefix: true,
  });

  const outputPath = `./${appName}.ts`;
  const output = astToString(ast);

  console.log(`Outputting to ${outputPath}`);
  fs.writeFileSync(outputPath, output);
}

generate_types(process.argv[2]);
