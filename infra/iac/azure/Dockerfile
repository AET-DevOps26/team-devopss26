FROM hashicorp/terraform:latest
LABEL authors="awudy"

COPY . .

RUN terraform init

ENTRYPOINT ["terraform", "plan"]
