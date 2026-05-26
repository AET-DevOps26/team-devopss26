terraform {
  required_providers {
    azurerm = {
      source  = "hashicorp/azurerm"
      version = "4.74.0"
    }
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