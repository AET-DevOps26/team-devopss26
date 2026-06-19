# This output makes the public IP address of the virtual machine easily accessible
# after running `terraform apply`. It can be queried with `terraform output`.
output "vm_public_ip" {
  description = "The public IP address of the devops-vm."
  value       = azurerm_public_ip.pub-ip.ip_address
}
