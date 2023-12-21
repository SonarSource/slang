load("github.com/SonarSource/cirrus-modules@v2", "load_features")
load("cirrus", "env", "fs", "yaml")


def main(ctx):
    if env.get("CIRRUS_REPO_FULL_NAME") == 'SonarSource/slang-enterprise':
        features = yaml.dumps(load_features(ctx, only_if=dict()))
        doc = fs.read("private/.cirrus.yml")
        return features + doc
    else:
        return fs.read(".cirrus-public.yml")
