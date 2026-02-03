{
  description = "Java dev shell";

  inputs = {
    nixpkgs.url = "github:nixos/nixpkgs/nixos-unstable";

    # Tools
    nvim-nix.url = "github:JacksStuff0905/nvim-nix";
    
  };

  outputs =
    {
      self,
      nixpkgs,
      home-manager,
      ...
    }@inputs:
    let
      system = "x86_64-linux";
      pkgs = nixpkgs.legacyPackages.${system};
      lib = pkgs.lib;
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        buildInputs = let
            nvim = inputs.nvim-nix.packages."${system}".full;
          in [
          nvim
          pkgs.zsh
          pkgs.jdk
        ];

        shellHook = ''
          echo "Java development env loaded."
          exec zsh
        '';
      };
    };
}
