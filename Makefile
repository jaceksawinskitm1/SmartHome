main:
	cd src; javac --enable-preview --source 23 -d ../out/ SmartHomeLauncher.java
	cd out; java --enable-preview SmartHomeLauncher
