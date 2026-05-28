variable "ansible-ssh-priv-key-path" {
  description = "The path to the SSH private key used to manage the Azure VM"
  sensitive = true
  type = string
}
