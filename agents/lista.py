import google.genai as genai

client = genai.Client(api_key="AIzaSyBXkTEmQDLiM1FVYn-hPH7owCwwjLWfWaw")
for model in client.models.list():
    print(model.name)