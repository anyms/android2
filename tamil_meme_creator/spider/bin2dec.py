binary = input("binary > ")
reversed_binary = ""

for n in binary:
    reversed_binary = n + reversed_binary

start = 0
result = 0

for n in reversed_binary:
    result = result + (int(n) * (2**start))
    start = start + 1

print("The decimal is: {}".format(result))
