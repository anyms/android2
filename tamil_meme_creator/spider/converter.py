def bin2dec(binary):
    reversed_binary = ""
    for n in binary:
        reversed_binary = n + reversed_binary

    start = 0
    result = 0

    for n in reversed_binary:
        result = result + (int(n) * (2**start))
        start = start + 1
    return result


def dec2bin(decimal):
    dec = int(decimal)
    start = 0
    num = 2**start
    decs = []
    result = ""

    while num <= dec:
        decs.insert(0, num)
        start = start + 1
        num = 2**start

    for d in decs:
        if d <= dec:
            dec -= d
            result += "1"
        else:
            result += "0"

    return result


print("1. Binary to Decimal")
print("2. Decimal to Binary")
inp = input("\nOperation: ")
if inp == "1":
    dec = bin2dec(input("\nBinary: "))
    binary = dec2bin(dec)
    print("\n\nBINARY: {}".format(binary))
    print("DECIMAL: {}".format(dec))
elif inp == "2":
    binary = dec2bin(input("\nDecimal: "))
    dec = bin2dec(binary)
    print("\n\nBINARY: {}".format(binary))
    print("DECIMAL: {}".format(dec))
