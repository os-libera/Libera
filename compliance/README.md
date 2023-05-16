# Open-source compliance discovered by FOSSLight Scanner

This directory includes the open-source compliance of Libera found by FossLight.

The FOSSLight Scanner is composed of a Prechecker, Dependency Scanner, Source Code Scanner, and Binary Scanner. The scanning process is carried out in the following order:

1. **Prechecker**
   - Checks the copyright and license rule within the source code

2. **Dependency Scanner**
   - A tool that extracts open-source information while recursively traversing the dependencies of the package manager

3. **Source Scanner**
   - Uses ScanCode to search for strings in the source code and find copyright and license phrases

4. **Binary Scanner**
   - A tool that extracts binary files and lists of binary files. If there is open-source information included in the searched binary in the database, it outputs it

The verification was done through fosslight v.1.7.11 in a Python 3.8 virtualenv environment on Ubuntu 20.04.6 LTS (Linux 5.4.0-148-generic).

The results include information such as the path of the source code, Open Source Software (OSS) information, License information, download path, homepage, Copyright, etc.

