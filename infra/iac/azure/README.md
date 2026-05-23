# Setup Azure infrastructure
To be able to use Terraform on your device, perform the following steps:
1. Prepare your Azure account as explained in Azure4Students lecture
2. Install the Azure CLI
3. Run `az login` and log into the TUM tenant
4. Run `az account set --subscription <tum-subscription-id>`
5. Run `az ad sp create-for-rbac --role="Contributor" --scopes="/subscriptions/<tum-subscription-id>"` to receive
    ```
       {
          "appId": "<app-id>",
          "displayName": "<display-name>",
          "password": "<password>",
          "tenant": "<tenant>"
       }
    ```
    which are used to give Terraform the permission to act inside Azure on your behalf
6. Create a `.env` file and insert the following
    ```
    ARM_CLIENT_ID=<app-id>
    ARM_CLIENT_SECRET=<password>
    ARM_SUBSCRIPTION_ID=<tum-subscription-id>
    ARM_TENANT_ID=<tenant>
    ```