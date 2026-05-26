# The virtual network (VNet) defines a private network space in Azure for all services.
resource "azurerm_virtual_network" "virtual-net" {
  resource_group_name = azurerm_resource_group.rg_root.name
  location            = "Sweden Central"
  name                = "devops-vnet"
  address_space       = ["10.0.0.0/16"]
}

# The subnet divides the VNet into smaller, manageable network segments.
# The VM will be placed inside this subnet.
resource "azurerm_subnet" "subnet" {
  virtual_network_name = azurerm_virtual_network.virtual-net.name
  resource_group_name  = azurerm_resource_group.rg_root.name
  name                 = "devops-subnet"
  address_prefixes     = ["10.0.2.0/24"]
}

# The firewall (Network Security Group) configures allowed inbound and outbound traffic rules.
resource "azurerm_network_security_group" "net-sec-group" {
  resource_group_name = azurerm_resource_group.rg_root.name
  location            = "Sweden Central"
  name                = "devops-net-sec-group"

  # Allows SSH access from anywhere on the internet. Secured by SSH keys on the VM.
  security_rule {
    name                       = "Allow-SSH"
    priority                   = 100
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "22"
    source_address_prefix      = "Internet"
    destination_address_prefix = "*"
  }

  # Allows HTTP traffic for the web server (e.g., for Caddy's certificate challenge).
  security_rule {
    name                       = "Allow-HTTP"
    priority                   = 110
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "80"
    source_address_prefix      = "Internet"
    destination_address_prefix = "*"
  }

  # Allows HTTPS traffic for the actual web application.
  security_rule {
    name                       = "Allow-HTTPS"
    priority                   = 120
    direction                  = "Inbound"
    access                     = "Allow"
    protocol                   = "Tcp"
    source_port_range          = "*"
    destination_port_range     = "443"
    source_address_prefix      = "Internet"
    destination_address_prefix = "*"
  }
}

# Sets up a static public IP address to make the VM reachable from the internet.
resource "azurerm_public_ip" "pub-ip" {
  resource_group_name = azurerm_resource_group.rg_root.name
  location            = "Sweden Central"
  name                = "devops-pub-ip"
  allocation_method   = "Static"
  sku                 = "Standard"
}

# This resource represents the virtual Network Interface Card (NIC) for the VM.
# It connects the VM to the subnet and the public IP address.
resource "azurerm_network_interface" "net-interface" {
  resource_group_name = azurerm_resource_group.rg_root.name
  location            = "Sweden Central"
  name                = "devops-net-interface"

  ip_configuration {
    public_ip_address_id          = azurerm_public_ip.pub-ip.id
    subnet_id                     = azurerm_subnet.subnet.id
    name                          = "devops-ip-config"
    private_ip_address_allocation = "Dynamic"
  }
}

# This crucial block associates the firewall (NSG) with the network interface (NIC).
# Without this, the firewall rules would have no effect.
resource "azurerm_network_interface_security_group_association" "nsg-assoc" {
  network_interface_id      = azurerm_network_interface.net-interface.id
  network_security_group_id = azurerm_network_security_group.net-sec-group.id
}