resource "azurerm_linux_virtual_machine" "vm" {
  name                = "devops-vm"
  resource_group_name = azurerm_resource_group.rg_root.name
  location            = "Sweden Central"
  
  disable_password_authentication = true
  admin_username                  = "devops-admin"
  admin_ssh_key {
    username   = "devops-admin"
    public_key = file("keys/pipeline.pub")
  }
  admin_ssh_key {
    username   = "devops-admin"
    public_key = file("keys/alex.pub")
  }
  admin_ssh_key {
    username   = "devops-admin"
    public_key = file("keys/werner.pub")
  }
  
  network_interface_ids           = [azurerm_network_interface.net-interface.id]
  size                            = "Standard_B2s_v2"

  os_disk {
    name                 = "devops-vm-os-disk"
    caching              = "ReadOnly"
    storage_account_type = "Standard_LRS"
  }

  source_image_reference {
    publisher = "Canonical"
    offer     = "0001-com-ubuntu-server-jammy"
    sku       = "22_04-lts-gen2"
    version   = "latest"
  }
}

resource "azurerm_dev_test_global_vm_shutdown_schedule" "vm_shutdown" {
  virtual_machine_id = azurerm_linux_virtual_machine.vm.id
  location           = azurerm_linux_virtual_machine.vm.location
  enabled            = true

  daily_recurrence_time = "0300"
  timezone              = "W. Europe Standard Time"

  notification_settings {
    enabled         = false
  }
}
