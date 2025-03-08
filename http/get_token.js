const fs = require("fs");
const tokenFileName = "ndla_m2m_token.json";
const environment = request.environment.get("env");
const token_environment = get_token_environment(environment);
const auth0_url = request.environment.get("AUTH0_ENDPOINT");
const auth0_client_id = request.environment.get("AUTH0_CLIENT_ID");
const auth0_client_secret = request.environment.get("AUTH0_CLIENT_SECRET");
const auth0_audience = request.environment.get("AUTH0_AUDIENCE");
const existingTokenFile = read_token_file();
// const test_environments = ["dev", "test", "docker"];

async function refetch_token() {
  console.log(`Fetching token for ${environment}: ${auth0_url}...`);
  const fetchedAt = Date.now();

  const getTokenRequest = await fetch(auth0_url, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      grant_type: "client_credentials",
      client_id: auth0_client_id,
      client_secret: auth0_client_secret,
      audience: auth0_audience,
    }),
  });
  const jsonBody = await getTokenRequest.json();
  const expires = jsonBody.expires_in * 1000 + Date.now();
  const token = {
    expiresIn: jsonBody.expiresIn,
    token: jsonBody.access_token,
    environment,
    expires,
    fetchedAt,
  };

  const newTokenFile = { ...existingTokenFile };
  newTokenFile[token_environment] = token;
  fs.writeFileSync(tokenFileName, JSON.stringify(newTokenFile));

  return newTokenFile;
}

function validate_environment(current_env, token_env) {
  if (!current_env || !token_env) return false;
  return (
    get_token_environment(current_env) === get_token_environment(token_env)
  );
}

function get_token_environment(env) {
  switch (env) {
    case "prod":
      return "prod";
    case "staging":
      return "staging";
    default:
      return "test";
  }
}

function token_is_valid(token_file) {
  const token_env = get_token_environment(environment);
  const token_obj = token_file[token_env];
  if (!token_obj) return false;

  const now = Date.now();
  const margin = 10;
  const is_expired = token_obj.expires < now - margin;
  const is_correct_environment = validate_environment(
    environment,
    token_obj.environment,
  );

  return !is_expired && is_correct_environment;
}

function read_token_file() {
  try {
    const tokenFile = fs.readFileSync(tokenFileName, "utf-8").trim();
    return JSON.parse(tokenFile);
  } catch (e) {
    console.log("No tokenfile found");
  }
}

async function get_stored_or_create_token() {
  try {
    if (token_is_valid(existingTokenFile)) {
      return existingTokenFile;
    }
  } catch (e) {
    console.log(e);
  }
  return await refetch_token();
}

async function update_vars() {
  const token = await get_stored_or_create_token();
  request.variables.set("token", token[token_environment].token);
}

update_vars();
