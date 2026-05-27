terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "4.74.0"
    }
  }

  backend "azurerm" {
    resource_group_name  = "devops-rg"
    storage_account_name = "tumdevopss26tfstate"
    container_name       = "tfstate"
    key                  = "terraform.tfstate"
  }

  required_version = ">= 1.15.4"
}

provider "azurerm" {
  features {}
}

resource "azurerm_resource_group" "rg_root" {
  location = "East US"
  name     = "devops-rg"
}