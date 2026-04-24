/*
 * Part of NDLA taxonomy-api
 * Copyright (C) 2025 NDLA
 *
 * See LICENSE
 *
 */

import fs from "node:fs";
import openapiTS, { astToString } from "openapi-typescript";

async function generate_types() {
  const jsonFile = `./openapi.json`;
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

  const outputPath = `./taxonomy-api.ts`;

  const output = astToString(ast);

  console.log(`Outputting to ${outputPath}`);

  fs.writeFileSync(outputPath, output);
}

generate_types();
